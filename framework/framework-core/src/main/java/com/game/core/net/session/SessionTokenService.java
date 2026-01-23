package com.game.core.net.session;

import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session Token 服务
 * <p>
 * 基于 Redis 存储 Session Token，支持多机部署下的断线重连：
 * <ul>
 *     <li>玩家登录时生成 Token 并存储到 Redis</li>
 *     <li>断线重连时通过 Token 恢复会话状态</li>
 *     <li>Token 有效期内可在任意服务器重连</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTokenService {

    private final RedisService redisService;

    /**
     * Token 前缀
     */
    private static final String TOKEN_KEY_PREFIX = "session:token:";

    /**
     * 角色 Token 映射前缀
     */
    private static final String ROLE_TOKEN_KEY_PREFIX = "session:role:";

    /**
     * Token 有效期 (默认 5 分钟)
     */
    @Value("${game.session.token-expire-seconds:300}")
    private int tokenExpireSeconds;

    /**
     * 生成并存储 Token
     *
     * @param roleId    角色 ID
     * @param accountId 账号 ID
     * @param serverId  服务器 ID
     * @return 生成的 Token
     */
    public String generateToken(long roleId, long accountId, int serverId) {
        // 生成 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 存储 Token 信息
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("roleId", String.valueOf(roleId));
        tokenData.put("accountId", String.valueOf(accountId));
        tokenData.put("serverId", String.valueOf(serverId));
        tokenData.put("createTime", String.valueOf(System.currentTimeMillis()));

        String tokenKey = TOKEN_KEY_PREFIX + token;
        redisService.hSetAll(tokenKey, tokenData);
        redisService.expire(tokenKey, tokenExpireSeconds);

        // 存储角色 -> Token 映射 (用于踢出旧连接)
        String roleTokenKey = ROLE_TOKEN_KEY_PREFIX + roleId;
        redisService.set(roleTokenKey, token, Duration.ofSeconds(tokenExpireSeconds));

        log.debug("生成 Session Token: roleId={}, token={}", roleId, token);
        return token;
    }

    /**
     * 验证并获取 Token 信息
     *
     * @param token Token
     * @return Token 信息，验证失败返回 null
     */
    public TokenInfo validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        String tokenKey = TOKEN_KEY_PREFIX + token;
        Map<String, String> tokenData = redisService.hGetAll(tokenKey);

        if (tokenData == null || tokenData.isEmpty()) {
            log.debug("Token 不存在或已过期: token={}", token);
            return null;
        }

        try {
            TokenInfo info = new TokenInfo();
            info.setToken(token);
            info.setRoleId(Long.parseLong(tokenData.get("roleId")));
            info.setAccountId(Long.parseLong(tokenData.get("accountId")));
            info.setServerId(Integer.parseInt(tokenData.get("serverId")));
            info.setCreateTime(Long.parseLong(tokenData.get("createTime")));
            return info;
        } catch (Exception e) {
            log.error("解析 Token 信息失败: token={}", token, e);
            return null;
        }
    }

    /**
     * 刷新 Token 有效期
     */
    public void refreshToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        if (redisService.exists(tokenKey)) {
            redisService.expire(tokenKey, tokenExpireSeconds);
        }
    }

    /**
     * 使 Token 失效
     */
    public void invalidateToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        String tokenKey = TOKEN_KEY_PREFIX + token;
        redisService.delete(tokenKey);
        log.debug("Token 已失效: token={}", token);
    }

    /**
     * 使角色的所有 Token 失效
     */
    public void invalidateByRoleId(long roleId) {
        String roleTokenKey = ROLE_TOKEN_KEY_PREFIX + roleId;
        String token = redisService.get(roleTokenKey);

        if (token != null) {
            invalidateToken(token);
            redisService.delete(roleTokenKey);
        }
    }

    /**
     * 获取角色当前的 Token
     */
    public String getTokenByRoleId(long roleId) {
        return redisService.get(ROLE_TOKEN_KEY_PREFIX + roleId);
    }

    /**
     * Token 信息
     */
    @lombok.Data
    public static class TokenInfo {
        private String token;
        private long roleId;
        private long accountId;
        private int serverId;
        private long createTime;
    }
}
