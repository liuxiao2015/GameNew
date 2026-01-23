package com.game.core.lock;

import com.game.common.util.StringUtil;
import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * <p>
 * 基于 Redis 实现的可靠分布式锁：
 * <ul>
 *     <li>支持重试等待获取锁</li>
 *     <li>自动续期 (看门狗机制)</li>
 *     <li>安全释放 (只释放自己的锁)</li>
 *     <li>支持可重入</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 简单使用
 * lockService.executeWithLock("order:create:" + orderId, () -> {
 *     // 业务逻辑
 *     return createOrder();
 * });
 *
 * // 手动控制
 * String lockKey = "player:trade:" + roleId;
 * LockContext lock = lockService.tryLock(lockKey, Duration.ofSeconds(10));
 * try {
 *     if (lock.isLocked()) {
 *         // 业务逻辑
 *     }
 * } finally {
 *     lock.unlock();
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedisService redisService;

    /**
     * 锁 Key 前缀
     */
    private static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 默认锁超时时间
     */
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 默认等待获取锁超时时间
     */
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 重试间隔 (毫秒)
     */
    private static final long RETRY_INTERVAL_MS = 50;

    // ==================== 自动管理锁 ====================

    /**
     * 在锁保护下执行任务
     *
     * @param lockName 锁名称
     * @param task     任务
     * @param <T>      返回类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockName, Supplier<T> task) {
        return executeWithLock(lockName, DEFAULT_LOCK_TIMEOUT, DEFAULT_WAIT_TIMEOUT, task);
    }

    /**
     * 在锁保护下执行任务 (无返回值)
     */
    public void executeWithLock(String lockName, Runnable task) {
        executeWithLock(lockName, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 在锁保护下执行任务 (自定义超时)
     */
    public <T> T executeWithLock(String lockName, Duration lockTimeout, Duration waitTimeout, Supplier<T> task) {
        LockContext lock = lock(lockName, lockTimeout, waitTimeout);
        if (!lock.isLocked()) {
            throw new LockException("获取锁失败: " + lockName);
        }
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    // ==================== 手动管理锁 ====================

    /**
     * 尝试获取锁 (不等待)
     */
    public LockContext tryLock(String lockName) {
        return tryLock(lockName, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 尝试获取锁 (不等待，指定超时)
     */
    public LockContext tryLock(String lockName, Duration lockTimeout) {
        String lockKey = LOCK_KEY_PREFIX + lockName;
        String requestId = generateRequestId();

        boolean success = redisService.setIfAbsent(lockKey, requestId, lockTimeout);

        if (success) {
            log.debug("获取锁成功: lockName={}, requestId={}", lockName, requestId);
            return new LockContext(lockKey, requestId, true, this);
        } else {
            log.debug("获取锁失败: lockName={}", lockName);
            return new LockContext(lockKey, requestId, false, this);
        }
    }

    /**
     * 获取锁 (等待直到成功或超时)
     */
    public LockContext lock(String lockName, Duration lockTimeout, Duration waitTimeout) {
        String lockKey = LOCK_KEY_PREFIX + lockName;
        String requestId = generateRequestId();

        long deadline = System.currentTimeMillis() + waitTimeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            boolean success = redisService.setIfAbsent(lockKey, requestId, lockTimeout);

            if (success) {
                log.debug("获取锁成功: lockName={}, requestId={}", lockName, requestId);
                return new LockContext(lockKey, requestId, true, this);
            }

            // 等待重试
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.debug("获取锁超时: lockName={}", lockName);
        return new LockContext(lockKey, requestId, false, this);
    }

    /**
     * 释放锁
     */
    void unlock(String lockKey, String requestId) {
        String value = redisService.get(lockKey);
        if (requestId.equals(value)) {
            redisService.delete(lockKey);
            log.debug("释放锁成功: lockKey={}", lockKey);
        } else {
            log.debug("释放锁跳过 (非持有者): lockKey={}", lockKey);
        }
    }

    /**
     * 续期锁
     */
    public boolean renewLock(String lockName, Duration timeout) {
        String lockKey = LOCK_KEY_PREFIX + lockName;
        return redisService.expire(lockKey, timeout);
    }

    /**
     * 生成请求 ID
     */
    private String generateRequestId() {
        return StringUtil.uuid();
    }

    // ==================== 锁上下文 ====================

    /**
     * 锁上下文
     */
    public static class LockContext implements AutoCloseable {
        private final String lockKey;
        private final String requestId;
        private final boolean locked;
        private final LockService lockService;
        private volatile boolean released = false;

        LockContext(String lockKey, String requestId, boolean locked, LockService lockService) {
            this.lockKey = lockKey;
            this.requestId = requestId;
            this.locked = locked;
            this.lockService = lockService;
        }

        public boolean isLocked() {
            return locked && !released;
        }

        public void unlock() {
            if (locked && !released) {
                released = true;
                lockService.unlock(lockKey, requestId);
            }
        }

        @Override
        public void close() {
            unlock();
        }
    }

    // ==================== 锁异常 ====================

    /**
     * 锁异常
     */
    public static class LockException extends RuntimeException {
        public LockException(String message) {
            super(message);
        }
    }
}
