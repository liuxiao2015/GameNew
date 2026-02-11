package com.game.core.limit;

import com.game.data.redis.RedisService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流服务 (本地 Bucket4j + 分布式 Redis)
 * <p>
 * 本地限流使用 Bucket4j 令牌桶，分布式限流使用 Redis 滑动窗口。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 本地令牌桶限流 (每秒 100 个请求)
 * if (rateLimiter.tryAcquire("api:login", 100)) {
 *     // 处理请求
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
     * 本地 Bucket4j 桶缓存
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Redis Key 前缀
     */
    private static final String LIMIT_KEY_PREFIX = "limit:";

    // ==================== 本地令牌桶限流 (Bucket4j) ====================

    /**
     * 尝试获取令牌 (本地限流)
     *
     * @param key           限流 Key
     * @param ratePerSecond 每秒允许的请求数
     * @return true=获取成功
     */
    public boolean tryAcquire(String key, int ratePerSecond) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(ratePerSecond));
        return bucket.tryConsume(1);
    }

    /**
     * 尝试获取多个令牌
     */
    public boolean tryAcquire(String key, int ratePerSecond, int permits) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(ratePerSecond));
        return bucket.tryConsume(permits);
    }

    /**
     * 创建 Bucket4j 桶
     */
    private Bucket createBucket(int ratePerSecond) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(ratePerSecond)
                .refillGreedy(ratePerSecond, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // ==================== 分布式限流 (Redis 滑动窗口) ====================

    /**
     * 分布式滑动窗口限流
     *
     * @param key      限流 Key
     * @param maxCount 窗口内最大请求数
     * @param window   时间窗口
     * @return true=未超限
     */
    public boolean tryAcquireDistributed(String key, int maxCount, Duration window) {
        String redisKey = LIMIT_KEY_PREFIX + key;
        long windowSeconds = window.toSeconds();

        Long count = redisService.increment(redisKey);

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

    public boolean tryPlayerLimit(long roleId, String action, int maxCount, Duration window) {
        String key = "player:" + roleId + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }

    public boolean tryIpLimit(String ip, String action, int maxCount, Duration window) {
        String key = "ip:" + ip + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }

    public boolean tryDeviceLimit(String deviceId, String action, int maxCount, Duration window) {
        String key = "device:" + deviceId + ":" + action;
        return tryAcquireDistributed(key, maxCount, window);
    }
}
