package com.game.service.gm.service;

import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.IdGenerator;
import com.game.data.redis.RedisService;
import com.game.service.gm.dto.LoginResponse;
import com.game.service.gm.entity.GmAccount;
import com.game.service.gm.repository.GmAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * GM 认证服务
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmAuthService {

    /**
     * Token 缓存前缀
     */
    private static final String TOKEN_KEY = "gm:token:";
    
    /**
     * Token 有效期 (2 小时)
     */
    private static final long TOKEN_EXPIRE = 7200;

    private final GmAccountRepository accountRepository;
    private final RedisService redisService;
    private final IdGenerator idGenerator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录
     */
    public Result<LoginResponse> login(String username, String password, String ip) {
        // 查询账号
        Optional<GmAccount> accountOpt = accountRepository.findByUsername(username);
        if (accountOpt.isEmpty()) {
            log.warn("GM 登录失败 - 账号不存在: username={}, ip={}", username, ip);
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        GmAccount account = accountOpt.get();

        // 检查状态
        if (account.getStatus() != 1) {
            log.warn("GM 登录失败 - 账号禁用: username={}, ip={}", username, ip);
            return Result.fail(ErrorCode.ACCOUNT_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(password, account.getPassword())) {
            log.warn("GM 登录失败 - 密码错误: username={}, ip={}", username, ip);
            return Result.fail(ErrorCode.PASSWORD_ERROR);
        }

        // 生成 Token
        String accessToken = generateToken();
        String refreshToken = generateToken();

        // 缓存 Token
        String tokenInfo = account.getId() + ":" + account.getRole();
        redisService.setEx(TOKEN_KEY + accessToken, tokenInfo, TOKEN_EXPIRE);
        redisService.setEx(TOKEN_KEY + "refresh:" + refreshToken, account.getId(), TOKEN_EXPIRE * 12);

        // 更新登录信息
        account.setLastLoginTime(System.currentTimeMillis());
        account.setLastLoginIp(ip);
        accountRepository.save(account);

        log.info("GM 登录成功: username={}, ip={}", username, ip);

        return Result.success(LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(TOKEN_EXPIRE)
            .username(account.getUsername())
            .nickname(account.getNickname())
            .role(account.getRole())
            .permissions(account.getPermissions())
            .build());
    }

    /**
     * 登出
     */
    public Result<Void> logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        redisService.delete(TOKEN_KEY + token);
        return Result.success();
    }

    /**
     * 刷新令牌
     */
    public Result<LoginResponse> refreshToken(String refreshToken) {
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        String accountId = redisService.get(TOKEN_KEY + "refresh:" + refreshToken);
        if (accountId == null) {
            return Result.fail(ErrorCode.TOKEN_INVALID);
        }

        Optional<GmAccount> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Result.fail(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        GmAccount account = accountOpt.get();

        // 生成新 Token
        String newAccessToken = generateToken();
        String newRefreshToken = generateToken();

        String tokenInfo = account.getId() + ":" + account.getRole();
        redisService.setEx(TOKEN_KEY + newAccessToken, tokenInfo, TOKEN_EXPIRE);
        redisService.setEx(TOKEN_KEY + "refresh:" + newRefreshToken, account.getId(), TOKEN_EXPIRE * 12);

        // 删除旧的刷新令牌
        redisService.delete(TOKEN_KEY + "refresh:" + refreshToken);

        return Result.success(LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(TOKEN_EXPIRE)
            .username(account.getUsername())
            .nickname(account.getNickname())
            .role(account.getRole())
            .permissions(account.getPermissions())
            .build());
    }

    /**
     * 验证 Token
     */
    public GmAccount validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String tokenInfo = redisService.get(TOKEN_KEY + token);
        if (tokenInfo == null) {
            return null;
        }

        String[] parts = tokenInfo.split(":");
        if (parts.length < 1) {
            return null;
        }

        return accountRepository.findById(parts[0]).orElse(null);
    }

    /**
     * 生成 Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
            Long.toHexString(idGenerator.nextId());
    }
}
