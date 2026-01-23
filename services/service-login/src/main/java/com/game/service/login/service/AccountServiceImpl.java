package com.game.service.login.service;

import com.game.api.login.AccountService;
import com.game.api.login.dto.AccountDTO;
import com.game.api.login.dto.ServerDTO;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.IdGenerator;
import com.game.data.redis.RedisService;
import com.game.service.login.entity.Account;
import com.game.service.login.entity.GameServer;
import com.game.service.login.repository.AccountRepository;
import com.game.service.login.repository.GameServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 账号服务实现
 * <p>
 * 支持多种登录方式：游客、账号密码、Google、Facebook、Apple
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String TOKEN_KEY_PREFIX = "account:token:";
    private static final String ACCOUNT_TOKEN_KEY_PREFIX = "account:id:token:";
    private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 3600; // 7 天

    private final AccountRepository accountRepository;
    private final GameServerRepository gameServerRepository;
    private final RedisService redisService;
    private final IdGenerator idGenerator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 游客登录 ====================

    @Override
    public Result<AccountDTO> guestLogin(String deviceId, String platform, String channel) {
        log.info("游客登录: deviceId={}, platform={}, channel={}", deviceId, platform, channel);

        // 查找或创建游客账号
        Optional<Account> accountOpt = accountRepository.findByDeviceIdAndPlatformType(deviceId, LOGIN_TYPE_GUEST);
        
        Account account;
        boolean isNew = false;
        
        if (accountOpt.isEmpty()) {
            // 创建新游客账号
            account = createGuestAccount(deviceId, platform, channel);
            isNew = true;
            log.info("创建游客账号: accountId={}, deviceId={}", account.getAccountId(), deviceId);
        } else {
            account = accountOpt.get();
            if (account.isBanned()) {
                return Result.fail(ErrorCode.ACCOUNT_BANNED);
            }
        }

        // 更新登录信息
        updateLoginInfo(account, deviceId);

        // 生成 Token
        String token = generateToken(account.getAccountId());

        return Result.success(buildAccountDTO(account, token, isNew));
    }

    // ==================== 账号密码登录/注册 ====================

    @Override
    public Result<AccountDTO> accountLogin(String username, String password, String deviceId) {
        log.info("账号登录: account={}", username);

        Optional<Account> accountOpt = accountRepository.findByUsername(username);
        if (accountOpt.isEmpty()) {
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = accountOpt.get();

        if (account.isBanned()) {
            return Result.fail(ErrorCode.ACCOUNT_BANNED);
        }

        // 验证密码
        if (!passwordEncoder.matches(password, account.getPassword())) {
            return Result.fail(ErrorCode.PASSWORD_ERROR);
        }

        // 更新登录信息
        updateLoginInfo(account, deviceId);

        // 生成 Token
        String token = generateToken(account.getAccountId());

        return Result.success(buildAccountDTO(account, token, false));
    }

    @Override
    public Result<AccountDTO> accountRegister(String username, String password, String deviceId, String email) {
        log.info("账号注册: account={}, email={}", username, email);

        // 检查账号是否存在
        if (accountRepository.findByUsername(username).isPresent()) {
            return Result.fail(ErrorCode.ACCOUNT_EXISTS);
        }

        // 创建账号
        Account account = new Account();
        account.setAccountId(generateAccountId());
        account.setPlatformType(LOGIN_TYPE_ACCOUNT);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setEmail(email);
        account.setDeviceId(deviceId);
        account.setStatus(1);
        account.setRoleIds(new ArrayList<>());
        account.setCreateTime(System.currentTimeMillis());
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);

        log.info("注册账号成功: accountId={}, username={}", account.getAccountId(), username);

        // 生成 Token
        String token = generateToken(account.getAccountId());

        return Result.success(buildAccountDTO(account, token, true));
    }

    // ==================== 第三方平台登录 ====================

    @Override
    public Result<AccountDTO> thirdPartyLogin(int loginType, String accessToken, String deviceId) {
        log.info("第三方登录: type={}", loginType);

        // 验证第三方 Token 并获取用户信息
        ThirdPartyUserInfo userInfo = verifyThirdPartyToken(loginType, accessToken);
        if (userInfo == null) {
            return Result.fail(ErrorCode.THIRD_PARTY_AUTH_FAILED);
        }

        // 查找或创建账号
        Optional<Account> accountOpt = accountRepository.findByPlatformTypeAndPlatformUserId(
                loginType, userInfo.userId);

        Account account;
        boolean isNew = false;

        if (accountOpt.isEmpty()) {
            // 创建新账号
            account = createThirdPartyAccount(loginType, userInfo, deviceId);
            isNew = true;
            log.info("创建第三方账号: accountId={}, platform={}, platformUserId={}",
                    account.getAccountId(), loginType, userInfo.userId);
        } else {
            account = accountOpt.get();
            if (account.isBanned()) {
                return Result.fail(ErrorCode.ACCOUNT_BANNED);
            }
        }

        // 更新登录信息
        updateLoginInfo(account, deviceId);

        // 生成 Token
        String token = generateToken(account.getAccountId());

        return Result.success(buildAccountDTO(account, token, isNew));
    }

    // ==================== Token 登录 ====================

    @Override
    public Result<AccountDTO> tokenLogin(String token, String deviceId) {
        log.debug("Token 登录: token={}", token);

        // 验证 Token
        String accountId = redisService.get(TOKEN_KEY_PREFIX + token);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = accountOpt.get();
        if (account.isBanned()) {
            return Result.fail(ErrorCode.ACCOUNT_BANNED);
        }

        // 更新登录信息
        updateLoginInfo(account, deviceId);

        // 刷新 Token
        redisService.expire(TOKEN_KEY_PREFIX + token, TOKEN_EXPIRE_SECONDS);

        return Result.success(buildAccountDTO(account, token, false));
    }

    // ==================== 账号绑定 ====================

    @Override
    public Result<Void> bindAccount(String accountId, String username, String password, String email) {
        log.info("绑定账号: accountId={}, username={}", accountId, username);

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 检查账号是否已存在
        if (accountRepository.findByUsername(username).isPresent()) {
            return Result.fail(ErrorCode.ACCOUNT_EXISTS);
        }

        Account account = accountOpt.get();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setEmail(email);
        account.setPlatformType(LOGIN_TYPE_ACCOUNT); // 升级为正式账号
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);

        log.info("绑定账号成功: accountId={}, username={}", accountId, username);
        return Result.success();
    }

    @Override
    public Result<Void> bindThirdParty(String accountId, int loginType, String accessToken) {
        log.info("绑定第三方账号: accountId={}, type={}", accountId, loginType);

        // 验证第三方 Token
        ThirdPartyUserInfo userInfo = verifyThirdPartyToken(loginType, accessToken);
        if (userInfo == null) {
            return Result.fail(ErrorCode.THIRD_PARTY_AUTH_FAILED);
        }

        // 检查是否已被绑定
        if (accountRepository.findByPlatformTypeAndPlatformUserId(loginType, userInfo.userId).isPresent()) {
            return Result.fail(ErrorCode.THIRD_PARTY_ALREADY_BOUND);
        }

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 保存绑定信息 (可以扩展为支持多绑定)
        Account account = accountOpt.get();
        // TODO: 添加绑定记录表
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);

        return Result.success();
    }

    // ==================== Token 验证 ====================

    @Override
    public Result<String> verifyToken(String token) {
        String accountId = redisService.get(TOKEN_KEY_PREFIX + token);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }
        return Result.success(accountId);
    }

    @Override
    public Result<String> refreshToken(String oldToken) {
        String accountId = redisService.get(TOKEN_KEY_PREFIX + oldToken);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }

        // 删除旧 Token
        redisService.delete(TOKEN_KEY_PREFIX + oldToken);

        // 生成新 Token
        String newToken = generateToken(accountId);

        return Result.success(newToken);
    }

    // ==================== 服务器列表 ====================

    @Override
    public Result<List<ServerDTO>> getServerList(String accountId) {
        List<GameServer> servers = gameServerRepository.findByOpenTrueOrderByServerIdDesc();

        // 获取账号在各服务器的角色信息
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        Set<Integer> serverIdsWithRole = new HashSet<>();
        if (accountOpt.isPresent()) {
            // TODO: 从角色表查询账号在哪些服务器有角色
        }

        List<ServerDTO> result = servers.stream()
                .map(server -> toServerDTO(server, serverIdsWithRole.contains(server.getServerId())))
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<ServerDTO> getRecommendedServer(String accountId) {
        List<GameServer> recommended = gameServerRepository.findByRecommendedTrueAndOpenTrueOrderByServerIdDesc();
        
        if (recommended.isEmpty()) {
            // 没有推荐服务器，返回最新的服务器
            List<GameServer> all = gameServerRepository.findByOpenTrueOrderByServerIdDesc();
            if (all.isEmpty()) {
                return Result.fail(ErrorCode.SERVER_NOT_FOUND);
            }
            return Result.success(toServerDTO(all.get(0), false));
        }

        return Result.success(toServerDTO(recommended.get(0), false));
    }

    // ==================== 辅助方法 ====================

    private Account createGuestAccount(String deviceId, String platform, String channel) {
        Account account = new Account();
        account.setAccountId(generateAccountId());
        account.setPlatformType(LOGIN_TYPE_GUEST);
        account.setPlatformUserId(deviceId);
        account.setDeviceId(deviceId);
        account.setStatus(1);
        account.setRoleIds(new ArrayList<>());
        account.setCreateTime(System.currentTimeMillis());
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);
        return account;
    }

    private Account createThirdPartyAccount(int loginType, ThirdPartyUserInfo userInfo, String deviceId) {
        Account account = new Account();
        account.setAccountId(generateAccountId());
        account.setPlatformType(loginType);
        account.setPlatformUserId(userInfo.userId);
        account.setDeviceId(deviceId);
        account.setStatus(1);
        account.setRoleIds(new ArrayList<>());
        account.setCreateTime(System.currentTimeMillis());
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);
        return account;
    }

    private void updateLoginInfo(Account account, String deviceId) {
        account.setLastLoginTime(System.currentTimeMillis());
        account.setDeviceId(deviceId);
        account.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(account);
    }

    private String generateAccountId() {
        return String.valueOf(idGenerator.nextId());
    }

    private String generateToken(String accountId) {
        String token = UUID.randomUUID().toString().replace("-", "") +
                Long.toHexString(System.currentTimeMillis());

        // 保存 Token -> AccountId 映射
        redisService.setEx(TOKEN_KEY_PREFIX + token, accountId, TOKEN_EXPIRE_SECONDS);

        // 保存 AccountId -> Token 映射 (用于踢下线)
        redisService.setEx(ACCOUNT_TOKEN_KEY_PREFIX + accountId, token, TOKEN_EXPIRE_SECONDS);

        return token;
    }

    private AccountDTO buildAccountDTO(Account account, String token, boolean isNew) {
        return AccountDTO.builder()
                .accountId(account.getAccountId())
                .token(token)
                .tokenExpireTime(System.currentTimeMillis() + TOKEN_EXPIRE_SECONDS * 1000)
                .newAccount(isNew)
                .loginType(account.getPlatformType())
                .lastLoginServerId(0) // TODO: 从账号数据获取
                .build();
    }

    private ServerDTO toServerDTO(GameServer server, boolean hasRole) {
        return ServerDTO.builder()
                .serverId(server.getServerId())
                .serverName(server.getServerName())
                .status(server.getStatus())
                .openTime(server.getOpenTime())
                .groupId(server.getGroupId())
                .tag(server.getTag())
                .recommended(server.isRecommended())
                .host(server.getHost())
                .port(server.getPort())
                .build();
    }

    /**
     * 验证第三方 Token (需要对接各平台 SDK)
     */
    private ThirdPartyUserInfo verifyThirdPartyToken(int loginType, String accessToken) {
        // TODO: 实际应该调用各平台 API 验证
        switch (loginType) {
            case LOGIN_TYPE_GOOGLE:
                return verifyGoogleToken(accessToken);
            case LOGIN_TYPE_FACEBOOK:
                return verifyFacebookToken(accessToken);
            case LOGIN_TYPE_APPLE:
                return verifyAppleToken(accessToken);
            default:
                return null;
        }
    }

    private ThirdPartyUserInfo verifyGoogleToken(String accessToken) {
        // TODO: 调用 Google API 验证
        // 这里模拟返回
        return new ThirdPartyUserInfo("google_" + accessToken.hashCode(), "Google User", null);
    }

    private ThirdPartyUserInfo verifyFacebookToken(String accessToken) {
        // TODO: 调用 Facebook API 验证
        return new ThirdPartyUserInfo("fb_" + accessToken.hashCode(), "Facebook User", null);
    }

    private ThirdPartyUserInfo verifyAppleToken(String accessToken) {
        // TODO: 调用 Apple API 验证
        return new ThirdPartyUserInfo("apple_" + accessToken.hashCode(), "Apple User", null);
    }

    /**
     * 第三方用户信息
     */
    private record ThirdPartyUserInfo(String userId, String nickname, String avatar) {}
}
