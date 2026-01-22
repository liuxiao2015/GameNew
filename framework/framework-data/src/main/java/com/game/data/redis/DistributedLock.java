package com.game.data.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis 分布式锁
 * <p>
 * 使用 Lua 脚本保证原子性，支持可重入和自动续期
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 方式1: try-with-resources
 * try (var lock = distributedLock.tryLock("my-lock", Duration.ofSeconds(10))) {
 *     if (lock != null) {
 *         // 获取锁成功，执行业务逻辑
 *     }
 * }
 *
 * // 方式2: 回调方式
 * distributedLock.executeWithLock("my-lock", Duration.ofSeconds(10), () -> {
 *     // 执行业务逻辑
 *     return result;
 * });
 *
 * // 方式3: 手动管理
 * String lockToken = distributedLock.lock("my-lock", Duration.ofSeconds(10));
 * try {
 *     // 执行业务逻辑
 * } finally {
 *     distributedLock.unlock("my-lock", lockToken);
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 释放锁的 Lua 脚本 (保证原子性)
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """, Long.class);

    /**
     * 续期锁的 Lua 脚本
     */
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('pexpire', KEYS[1], ARGV[2])
            else
                return 0
            end
            """, Long.class);

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的 key
     * @param timeout 锁的超时时间
     * @return 锁的令牌，失败返回 null
     */
    public String lock(String lockKey, Duration timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        String token = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, token, timeout);

        if (Boolean.TRUE.equals(success)) {
            log.debug("获取锁成功: key={}, token={}", lockKey, token);
            return token;
        }

        log.debug("获取锁失败: key={}", lockKey);
        return null;
    }

    /**
     * 尝试获取锁 (带重试)
     *
     * @param lockKey    锁的 key
     * @param timeout    锁的超时时间
     * @param retryCount 重试次数
     * @param retryDelay 重试间隔
     * @return 锁的令牌，失败返回 null
     */
    public String lockWithRetry(String lockKey, Duration timeout, int retryCount, Duration retryDelay) {
        for (int i = 0; i <= retryCount; i++) {
            String token = lock(lockKey, timeout);
            if (token != null) {
                return token;
            }

            if (i < retryCount) {
                try {
                    Thread.sleep(retryDelay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的 key
     * @param token   锁的令牌
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String token) {
        if (token == null) {
            return false;
        }

        String fullKey = LOCK_PREFIX + lockKey;
        Long result = redisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(fullKey), token);

        boolean success = result != null && result == 1;
        if (success) {
            log.debug("释放锁成功: key={}", lockKey);
        } else {
            log.warn("释放锁失败（可能已过期或被其他进程持有）: key={}", lockKey);
        }
        return success;
    }

    /**
     * 续期锁
     *
     * @param lockKey 锁的 key
     * @param token   锁的令牌
     * @param timeout 新的超时时间
     * @return 是否续期成功
     */
    public boolean renew(String lockKey, String token, Duration timeout) {
        if (token == null) {
            return false;
        }

        String fullKey = LOCK_PREFIX + lockKey;
        Long result = redisTemplate.execute(RENEW_SCRIPT,
                Collections.singletonList(fullKey), token, String.valueOf(timeout.toMillis()));

        return result != null && result == 1;
    }

    /**
     * 尝试获取锁 (返回 AutoCloseable)
     *
     * @param lockKey 锁的 key
     * @param timeout 锁的超时时间
     * @return LockHandle，失败返回 null
     */
    public LockHandle tryLock(String lockKey, Duration timeout) {
        String token = lock(lockKey, timeout);
        if (token != null) {
            return new LockHandle(this, lockKey, token);
        }
        return null;
    }

    /**
     * 带锁执行
     *
     * @param lockKey  锁的 key
     * @param timeout  锁的超时时间
     * @param supplier 要执行的操作
     * @return 操作结果，获取锁失败返回 null
     */
    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> supplier) {
        String token = lock(lockKey, timeout);
        if (token == null) {
            log.warn("获取锁失败，无法执行操作: key={}", lockKey);
            return null;
        }

        try {
            return supplier.get();
        } finally {
            unlock(lockKey, token);
        }
    }

    /**
     * 带锁执行 (无返回值)
     *
     * @param lockKey 锁的 key
     * @param timeout 锁的超时时间
     * @param task    要执行的操作
     * @return 是否执行成功
     */
    public boolean executeWithLock(String lockKey, Duration timeout, Runnable task) {
        String token = lock(lockKey, timeout);
        if (token == null) {
            log.warn("获取锁失败，无法执行操作: key={}", lockKey);
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            unlock(lockKey, token);
        }
    }

    /**
     * 锁句柄 (支持 try-with-resources)
     */
    public static class LockHandle implements AutoCloseable {
        private final DistributedLock lock;
        private final String lockKey;
        private final String token;
        private boolean released = false;

        LockHandle(DistributedLock lock, String lockKey, String token) {
            this.lock = lock;
            this.lockKey = lockKey;
            this.token = token;
        }

        /**
         * 续期锁
         */
        public boolean renew(Duration timeout) {
            if (released) {
                return false;
            }
            return lock.renew(lockKey, token, timeout);
        }

        @Override
        public void close() {
            if (!released) {
                lock.unlock(lockKey, token);
                released = true;
            }
        }
    }
}
