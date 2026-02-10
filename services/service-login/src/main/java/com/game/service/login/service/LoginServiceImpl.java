package com.game.service.login.service;

import com.game.api.login.LoginResultDTO;
import com.game.api.login.LoginService;
import com.game.api.login.RoleBriefDTO;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.IdGenerator;
import com.game.data.redis.RedisService;
import com.game.entity.document.Account;
import com.game.entity.document.Role;
import com.game.entity.repository.AccountRepository;
import com.game.entity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 登录服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    /**
     * Token 缓存前缀
     */
    private static final String TOKEN_KEY = "login:token:";

    /**
     * Token 有效期 (24 小时)
     */
    private static final long TOKEN_EXPIRE = 86400;

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final RedisService redisService;
    private final IdGenerator idGenerator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Result<LoginResultDTO> login(String account, String password, String deviceId,
                                         String platform, String version, String channel) {
        // 查找账号
        Optional<Account> accountOpt = accountRepository.findByUsername(account);
        Account acc;

        if (accountOpt.isEmpty()) {
            // 自动创建账号
            acc = new Account();
            acc.setAccountId(UUID.randomUUID().toString().replace("-", ""));
            acc.setPlatformType(1);
            acc.setPlatformUserId(account);
            acc.setUsername(account);
            acc.setPassword(passwordEncoder.encode(password));
            acc.setDeviceId(deviceId);
            acc.setStatus(1);
            acc.setRoleIds(new ArrayList<>());
            acc.setCreateTime(System.currentTimeMillis());
            acc.setUpdateTime(System.currentTimeMillis());
            accountRepository.save(acc);

            log.info("创建新账号: accountId={}, username={}", acc.getAccountId(), account);
        } else {
            acc = accountOpt.get();

            // 检查封禁状态
            if (acc.isBanned()) {
                log.warn("账号被封禁: accountId={}", acc.getAccountId());
                return Result.fail(ErrorCode.ACCOUNT_BANNED);
            }

            // 验证密码
            if (!passwordEncoder.matches(password, acc.getPassword())) {
                return Result.fail(ErrorCode.PASSWORD_ERROR);
            }
        }

        // 更新登录信息
        acc.setLastLoginTime(System.currentTimeMillis());
        acc.setDeviceId(deviceId);
        acc.setUpdateTime(System.currentTimeMillis());
        accountRepository.save(acc);

        // 生成 Token
        String loginToken = generateToken();
        redisService.setEx(TOKEN_KEY + loginToken, acc.getAccountId(), TOKEN_EXPIRE);

        // 查询角色列表
        List<Role> roles = roleRepository.findByAccountIdAndStatus(acc.getAccountId(), 1);
        List<RoleBriefDTO> roleBriefs = roles.stream()
            .map(this::toRoleBrief)
            .collect(Collectors.toList());

        log.info("账号登录成功: accountId={}, roleCount={}", acc.getAccountId(), roles.size());

        return Result.success(LoginResultDTO.builder()
            .accountId(Long.parseLong(acc.getAccountId().substring(0, 16), 16) & Long.MAX_VALUE)
            .token(loginToken)
            .expireTime(System.currentTimeMillis() + TOKEN_EXPIRE * 1000)
            .roles(roleBriefs)
            .serverTime(System.currentTimeMillis())
            .build());
    }

    @Override
    public Result<Long> verifyToken(String token) {
        String accountId = redisService.get(TOKEN_KEY + token);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }
        return Result.success(Long.parseLong(accountId.substring(0, 16), 16) & Long.MAX_VALUE);
    }

    @Override
    public Result<String> refreshToken(String token) {
        String accountId = redisService.get(TOKEN_KEY + token);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }

        // 删除旧 Token
        redisService.delete(TOKEN_KEY + token);

        // 生成新 Token
        String newToken = generateToken();
        redisService.setEx(TOKEN_KEY + newToken, accountId, TOKEN_EXPIRE);

        return Result.success(newToken);
    }

    @Override
    public Result<List<RoleBriefDTO>> getRoleList(long accountId) {
        // 转换为字符串 accountId
        List<Role> roles = roleRepository.findByAccountIdAndStatus(
            Long.toHexString(accountId), 1);
        
        List<RoleBriefDTO> result = roles.stream()
            .map(this::toRoleBrief)
            .collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<RoleBriefDTO> createRole(long accountId, String roleName, int avatarId, int gender) {
        // 检查角色名
        if (roleName == null || roleName.length() < 2 || roleName.length() > 12) {
            return Result.fail(ErrorCode.ROLE_NAME_INVALID);
        }

        if (roleRepository.existsByRoleName(roleName)) {
            return Result.fail(ErrorCode.ROLE_NAME_EXISTS);
        }

        // 创建角色
        long roleId = idGenerator.nextId();
        Role role = new Role();
        role.setRoleId(roleId);
        role.setAccountId(Long.toHexString(accountId));
        role.setServerId(1); // 默认服务器
        role.setRoleName(roleName);
        role.setProfession(gender); // 暂时使用 gender 作为职业
        role.setLevel(1);
        role.setAvatarId(avatarId);
        role.setPower(100);
        role.setStatus(1);
        role.setCreateTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        roleRepository.save(role);

        log.info("创建角色成功: roleId={}, roleName={}", roleId, roleName);

        return Result.success(toRoleBrief(role));
    }

    @Override
    public Result<Void> enterGame(long accountId, long roleId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        Role role = roleOpt.get();
        if (role.getStatus() != 1) {
            return Result.fail(ErrorCode.ROLE_DELETED);
        }

        // 更新角色登录时间
        role.setLastLoginTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        roleRepository.save(role);

        log.info("进入游戏: accountId={}, roleId={}, roleName={}", accountId, roleId, role.getRoleName());
        return Result.success();
    }

    @Override
    public Result<Void> logout(long roleId) {
        log.info("登出游戏: roleId={}", roleId);
        return Result.success();
    }

    @Override
    public Result<Boolean> checkRoleName(String roleName) {
        if (roleName == null || roleName.length() < 2 || roleName.length() > 12) {
            return Result.success(false);
        }
        boolean exists = roleRepository.existsByRoleName(roleName);
        return Result.success(!exists);
    }

    /**
     * 生成 Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
            Long.toHexString(idGenerator.nextId());
    }

    /**
     * 转换为角色简要信息
     */
    private RoleBriefDTO toRoleBrief(Role role) {
        return RoleBriefDTO.builder()
            .roleId(role.getRoleId())
            .serverId(role.getServerId())
            .roleName(role.getRoleName())
            .level(role.getLevel())
            .avatarId(role.getAvatarId())
            .vipLevel(0)
            .lastLoginTime(role.getLastLoginTime())
            .createTime(role.getCreateTime())
            .build();
    }
}
