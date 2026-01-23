package com.game.core.consistency;

import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 幂等性保证服务
 * <p>
 * 确保跨服务操作的幂等性，防止重复执行：
 * <ul>
 *     <li>基于请求唯一标识去重</li>
 *     <li>支持自定义过期时间</li>
 *     <li>返回首次执行结果缓存</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用场景：
 * - 跨服道具发放
 * - 订单处理
 * - 任何需要幂等的操作
 * </pre>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 幂等发放道具
 * String requestId = "reward:" + orderId;
 * Result<Boolean> result = idempotentService.execute(requestId, Duration.ofHours(24), () -> {
 *     // 实际发放逻辑
 *     return giveReward(roleId, items);
 * });
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final RedisService redisService;

    /**
     * 幂等 Key 前缀
     */
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    /**
     * 默认过期时间 (24小时)
     */
    private static final Duration DEFAULT_EXPIRE = Duration.ofHours(24);

    /**
     * 执行幂等操作 (使用默认过期时间)
     *
     * @param requestId 请求唯一标识
     * @param action    要执行的操作
     * @param <T>       返回类型
     * @return 执行结果
     */
    public <T> IdempotentResult<T> execute(String requestId, Supplier<T> action) {
        return execute(requestId, DEFAULT_EXPIRE, action);
    }

    /**
     * 执行幂等操作
     *
     * @param requestId 请求唯一标识
     * @param expire    过期时间
     * @param action    要执行的操作
     * @param <T>       返回类型
     * @return 执行结果
     */
    public <T> IdempotentResult<T> execute(String requestId, Duration expire, Supplier<T> action) {
        String key = IDEMPOTENT_KEY_PREFIX + requestId;

        // 尝试获取幂等锁
        boolean acquired = redisService.setIfAbsent(key, "processing", expire);

        if (!acquired) {
            // 已经执行过
            log.debug("幂等检查: 请求已执行过, requestId={}", requestId);
            return IdempotentResult.duplicate();
        }

        try {
            // 首次执行
            T result = action.get();
            
            // 更新状态为已完成
            redisService.set(key, "completed", expire);
            
            log.debug("幂等执行: 首次执行成功, requestId={}", requestId);
            return IdempotentResult.success(result);

        } catch (Exception e) {
            // 执行失败，删除幂等标记，允许重试
            redisService.delete(key);
            log.error("幂等执行: 执行失败, requestId={}", requestId, e);
            return IdempotentResult.fail(e);
        }
    }

    /**
     * 检查是否已执行过
     */
    public boolean isExecuted(String requestId) {
        String key = IDEMPOTENT_KEY_PREFIX + requestId;
        return redisService.exists(key);
    }

    /**
     * 标记为已执行
     */
    public void markExecuted(String requestId, Duration expire) {
        String key = IDEMPOTENT_KEY_PREFIX + requestId;
        redisService.set(key, "completed", expire);
    }

    /**
     * 清除幂等标记 (用于需要重试的场景)
     */
    public void clearMark(String requestId) {
        String key = IDEMPOTENT_KEY_PREFIX + requestId;
        redisService.delete(key);
    }

    /**
     * 幂等执行结果
     */
    public static class IdempotentResult<T> {
        private final boolean firstExecution;
        private final boolean success;
        private final T result;
        private final Exception exception;

        private IdempotentResult(boolean firstExecution, boolean success, T result, Exception exception) {
            this.firstExecution = firstExecution;
            this.success = success;
            this.result = result;
            this.exception = exception;
        }

        public static <T> IdempotentResult<T> success(T result) {
            return new IdempotentResult<>(true, true, result, null);
        }

        public static <T> IdempotentResult<T> duplicate() {
            return new IdempotentResult<>(false, true, null, null);
        }

        public static <T> IdempotentResult<T> fail(Exception e) {
            return new IdempotentResult<>(true, false, null, e);
        }

        public boolean isFirstExecution() {
            return firstExecution;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isDuplicate() {
            return !firstExecution;
        }

        public T getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
}
