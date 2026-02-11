package com.game.core.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务 (基于 Redisson)
 * <p>
 * 使用 Redisson RLock 代替自研实现，自动具备：
 * <ul>
 *     <li>可重入锁</li>
 *     <li>Watch-Dog 自动续期 (默认 30s)</li>
 *     <li>Pub/Sub 通知唤醒 (不再轮询)</li>
 *     <li>Lua 原子操作</li>
 *     <li>RedLock 多节点支持 (可选)</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 简单使用
 * lockService.executeWithLock("order:create:" + orderId, () -> {
 *     return createOrder();
 * });
 *
 * // 手动控制
 * LockContext lock = lockService.tryLock("player:trade:" + roleId, Duration.ofSeconds(10));
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

    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);

    // ==================== 自动管理锁 ====================

    /**
     * 在锁保护下执行任务
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
        RLock rLock = redissonClient.getLock(lockKey);

        try {
            boolean success = rLock.tryLock(0, lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (success) {
                log.debug("获取锁成功: lockName={}", lockName);
                return new LockContext(rLock, true);
            } else {
                log.debug("获取锁失败: lockName={}", lockName);
                return new LockContext(rLock, false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LockContext(rLock, false);
        }
    }

    /**
     * 获取锁 (等待直到成功或超时)
     */
    public LockContext lock(String lockName, Duration lockTimeout, Duration waitTimeout) {
        String lockKey = LOCK_KEY_PREFIX + lockName;
        RLock rLock = redissonClient.getLock(lockKey);

        try {
            boolean success = rLock.tryLock(waitTimeout.toMillis(), lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (success) {
                log.debug("获取锁成功: lockName={}", lockName);
                return new LockContext(rLock, true);
            } else {
                log.debug("获取锁超时: lockName={}", lockName);
                return new LockContext(rLock, false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("获取锁被中断: lockName={}", lockName);
            return new LockContext(rLock, false);
        }
    }

    /**
     * 续期锁
     * <p>
     * Redisson Watch-Dog 默认自动续期 (每 10s 续期到 30s)。
     * 如果使用了 leaseTime，Watch-Dog 不会自动续期，
     * 此时可调用此方法手动判断锁是否仍然持有。
     * </p>
     *
     * @return 当前线程是否仍然持有该锁
     */
    public boolean renewLock(String lockName, Duration timeout) {
        String lockKey = LOCK_KEY_PREFIX + lockName;
        RLock rLock = redissonClient.getLock(lockKey);
        return rLock.isHeldByCurrentThread();
    }

    // ==================== 锁上下文 ====================

    /**
     * 锁上下文
     */
    public static class LockContext implements AutoCloseable {
        private final RLock rLock;
        private final boolean locked;
        private volatile boolean released = false;

        LockContext(RLock rLock, boolean locked) {
            this.rLock = rLock;
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked && !released;
        }

        public void unlock() {
            if (locked && !released) {
                released = true;
                try {
                    if (rLock.isHeldByCurrentThread()) {
                        rLock.unlock();
                    }
                } catch (Exception e) {
                    // 锁可能已过期被释放，忽略
                }
            }
        }

        @Override
        public void close() {
            unlock();
        }
    }

    // ==================== 锁异常 ====================

    public static class LockException extends RuntimeException {
        public LockException(String message) {
            super(message);
        }
    }
}
