package com.game.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 重试工具类
 * <p>
 * 提供自动重试机制，用于处理临时性失败
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 重试 3 次，每次间隔 100ms
 * String result = RetryUtil.retry(() -> callRemoteService(), 3, 100);
 *
 * // 自定义重试条件
 * Result data = RetryUtil.retry(() -> fetchData(),
 *     3, 200,
 *     e -> e instanceof TimeoutException);
 *
 * // 带默认值
 * String value = RetryUtil.retryOrDefault(() -> getValue(), 3, 100, "default");
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public final class RetryUtil {

    private RetryUtil() {
        // 禁止实例化
    }

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    /**
     * 默认重试间隔 (毫秒)
     */
    private static final long DEFAULT_DELAY_MS = 100;

    // ==================== 基础重试 ====================

    /**
     * 重试执行 (无返回值)
     *
     * @param task        任务
     * @param maxAttempts 最大尝试次数
     * @param delayMs     重试间隔 (毫秒)
     */
    public static void retry(Runnable task, int maxAttempts, long delayMs) {
        retry(() -> {
            task.run();
            return null;
        }, maxAttempts, delayMs);
    }

    /**
     * 重试执行 (有返回值)
     *
     * @param task        任务
     * @param maxAttempts 最大尝试次数
     * @param delayMs     重试间隔 (毫秒)
     * @param <T>         返回类型
     * @return 执行结果
     */
    public static <T> T retry(Callable<T> task, int maxAttempts, long delayMs) {
        return retry(task, maxAttempts, delayMs, e -> true);
    }

    /**
     * 重试执行 (自定义重试条件)
     *
     * @param task            任务
     * @param maxAttempts     最大尝试次数
     * @param delayMs         重试间隔 (毫秒)
     * @param retryCondition  重试条件 (返回 true 表示需要重试)
     * @param <T>             返回类型
     * @return 执行结果
     */
    public static <T> T retry(Callable<T> task, int maxAttempts, long delayMs,
                               Predicate<Exception> retryCondition) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxAttempts) {
            attempts++;
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;

                if (!retryCondition.test(e)) {
                    // 不符合重试条件，直接抛出
                    throw wrapException(e);
                }

                if (attempts < maxAttempts) {
                    log.warn("任务执行失败，准备重试: attempt={}/{}, error={}",
                            attempts, maxAttempts, e.getMessage());
                    sleep(delayMs);
                }
            }
        }

        log.error("任务执行失败，已达最大重试次数: maxAttempts={}", maxAttempts, lastException);
        throw wrapException(lastException);
    }

    // ==================== 便捷方法 ====================

    /**
     * 使用默认配置重试
     */
    public static <T> T retry(Callable<T> task) {
        return retry(task, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
    }

    /**
     * 使用默认配置重试 (无返回值)
     */
    public static void retry(Runnable task) {
        retry(task, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
    }

    /**
     * 重试执行，失败返回默认值
     */
    public static <T> T retryOrDefault(Callable<T> task, int maxAttempts, long delayMs, T defaultValue) {
        try {
            return retry(task, maxAttempts, delayMs);
        } catch (Exception e) {
            log.warn("任务执行失败，返回默认值: {}", defaultValue);
            return defaultValue;
        }
    }

    /**
     * 重试执行，失败返回 null
     */
    public static <T> T retryOrNull(Callable<T> task, int maxAttempts, long delayMs) {
        return retryOrDefault(task, maxAttempts, delayMs, null);
    }

    /**
     * 静默重试 (忽略异常)
     */
    public static void retrySilent(Runnable task, int maxAttempts, long delayMs) {
        try {
            retry(task, maxAttempts, delayMs);
        } catch (Exception e) {
            log.warn("静默重试失败: {}", e.getMessage());
        }
    }

    // ==================== 指数退避重试 ====================

    /**
     * 指数退避重试
     *
     * @param task          任务
     * @param maxAttempts   最大尝试次数
     * @param initialDelay  初始延迟 (毫秒)
     * @param maxDelay      最大延迟 (毫秒)
     * @param multiplier    延迟倍数
     * @param <T>           返回类型
     * @return 执行结果
     */
    public static <T> T retryWithBackoff(Callable<T> task, int maxAttempts,
                                          long initialDelay, long maxDelay, double multiplier) {
        int attempts = 0;
        long currentDelay = initialDelay;
        Exception lastException = null;

        while (attempts < maxAttempts) {
            attempts++;
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;

                if (attempts < maxAttempts) {
                    log.warn("任务执行失败，准备指数退避重试: attempt={}/{}, delay={}ms, error={}",
                            attempts, maxAttempts, currentDelay, e.getMessage());
                    sleep(currentDelay);
                    currentDelay = Math.min((long) (currentDelay * multiplier), maxDelay);
                }
            }
        }

        log.error("任务执行失败，已达最大重试次数: maxAttempts={}", maxAttempts, lastException);
        throw wrapException(lastException);
    }

    /**
     * 使用默认指数退避配置重试
     */
    public static <T> T retryWithBackoff(Callable<T> task) {
        return retryWithBackoff(task, 3, 100, 5000, 2.0);
    }

    // ==================== 辅助方法 ====================

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被中断", e);
        }
    }

    private static RuntimeException wrapException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }
}
