package com.game.core.limit;

import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流服务
 * <p>
 * 提供多种限流策略：
 * <ul>
 *     <li>令牌桶 - 本地限流，适合单机</li>
 *     <li>滑动窗口 - Redis 限流，适合分布式</li>
 *     <li>计数器 - 简单限流</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 本地令牌桶限流 (每秒 100 个请求)
 * if (rateLimiter.tryAcquire("api:login", 100)) {
 *     // 处理请求
 * } else {
 *     // 限流
 * }
 *
 * // 分布式滑动窗口限流 (1分钟内最多 10 次)
 * if (rateLimiter.tryAcquireDistributed("sms:" + phone, 10, Duration.ofMinutes(1))) {
 *     // 发送短信
 * }
 *
 * // 玩家操作限流
 * if (rateLimiter.tryPlayerLimit(roleId, "trade", 5, Duration.ofSeconds(10))) {
 *     // 执行交易
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisService redisService;

    /**
     * 本地令牌桶
     */
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    /**
     * Redis Key 前缀
     */
    private static final String LIMIT_KEY_PREFIX = "limit:";

    // ==================== 本地令牌桶限流 ====================

    /**
     * 尝试获取令牌 (本地限流)
     *
     * @param key       限流 Key
     * @param ratePerSecond 每秒允许的请求数
     * @return true=获取成功
     */
    public boolean tryAcquire(String key, int ratePerSecond) {
        TokenBucket bucket = tokenBuckets.computeIfAbsent(key,
                k -> new TokenBucket(ratePerSecond, ratePerSecond));
        return bucket.tryAcquire();
    }

    /**
     * 尝试获取多个令牌
     */
    public boolean tryAcquire(String key, int ratePerSecond, int permits) {
        TokenBucket bucket = tokenBuckets.computeIfAbsent(key,
                k -> new TokenBucket(ratePerSecond, ratePerSecond));
        return bucket.tryAcquire(permits);
    }

    // ==================== 分布式限流 ====================

    /**
     * 分布式滑动窗口限流
     *
     * @param key       限流 Key
     * @param maxCount  窗口内最大请求数
     * @param window    时间窗口
     * @return true=未超限
     */
    public boolean tryAcquireDistributed(String key, int maxCount, Duration window) {
        String redisKey = LIMIT_KEY_PREFIX + key;
        long windowSeconds = window.toSeconds();

        // 增加计数
        Long count = redisService.increment(redisKey);

        // 首次访问设置过期时间
        if (count != null && count == 1) {
            redisService.expire(redisKey, windowSeconds);
        }

        boolean allowed = count != null && count <= maxCount;

        if (!allowed) {
            log.debug("分布式限流触发: key={}, count={}, maxCount={}", key, count, maxCount);
        }

        return allowed;
    }

    /**
     * 获取当前计数
     */
    public long getDistributedCount(String key) {
        String redisKey = LIMIT_KEY_PREFIX + key;
        String value = redisService.get(redisKey);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * 重置分布式计数
     */
    public void resetDistributed(String key) {
        String redisKey = LIMIT_KEY_PREFIX + key;
        redisService.delete(redisKey);
    }

    // ==================== 玩家限流 ====================

    /**
     * 玩家操作限流
     *
     * @param roleId     角色 ID
     * @param action     操作类型
     * @param maxCount   最大次数
     * @param window     时间窗口
     * @return true=未超限
     */
    public boolean tryPlayerLimit(long roleId, String action, int maxCount, Duration window) {
        String key = "player:" + roleId + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }

    /**
     * IP 限流
     *
     * @param ip         IP 地址
     * @param action     操作类型
     * @param maxCount   最大次数
     * @param window     时间窗口
     * @return true=未超限
     */
    public boolean tryIpLimit(String ip, String action, int maxCount, Duration window) {
        String key = "ip:" + ip + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }

    /**
     * 设备限流
     */
    public boolean tryDeviceLimit(String deviceId, String action, int maxCount, Duration window) {
        String key = "device:" + deviceId + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }

    // ==================== 令牌桶实现 ====================

    /**
     * 令牌桶
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire() {
            return tryAcquire(1);
        }

        boolean tryAcquire(int permits) {
            refill();
            int current = tokens.get();
            if (current >= permits) {
                if (tokens.compareAndSet(current, current - permits)) {
                    return true;
                }
                // CAS 失败，重试
                return tryAcquire(permits);
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefillTime.get();
            long elapsed = now - last;

            if (elapsed > 1000) {
                int newTokens = (int) (elapsed / 1000 * refillRate);
                if (newTokens > 0 && lastRefillTime.compareAndSet(last, now)) {
                    int current = tokens.get();
                    int updated = Math.min(capacity, current + newTokens);
                    tokens.set(updated);
                }
            }
        }
    }
}
