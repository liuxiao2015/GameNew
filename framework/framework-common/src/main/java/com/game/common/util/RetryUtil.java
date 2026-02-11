package com.game.common.util;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * 重试工具类 (基于 Resilience4j Retry)
 * <p>
 * 保持原有 API 不变，内部委托 Resilience4j。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * String result = RetryUtil.retry(() -> callRemoteService(), 3, 100);
 * String value = RetryUtil.retryOrDefault(() -> getValue(), 3, 100, "default");
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public final class RetryUtil {

    private RetryUtil() {
    }

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_DELAY_MS = 100;

    // ==================== 基础重试 ====================

    /**
     * 重试执行 (无返回值)
     */
    public static void retry(Runnable task, int maxAttempts, long delayMs) {
        retry(() -> {
            task.run();
            return null;
        }, maxAttempts, delayMs);
    }

    /**
     * 重试执行 (有返回值)
     */
    public static <T> T retry(Callable<T> task, int maxAttempts, long delayMs) {
        return retry(task, maxAttempts, delayMs, e -> true);
    }

    /**
     * 重试执行 (自定义重试条件)
     */
    public static <T> T retry(Callable<T> task, int maxAttempts, long delayMs,
                               Predicate<Exception> retryCondition) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(delayMs))
                .retryOnException(e -> e instanceof Exception && retryCondition.test((Exception) e))
                .build();

        Retry retry = Retry.of("retryUtil-" + System.nanoTime(), config);

        retry.getEventPublisher().onRetry(event ->
                log.warn("任务执行失败，准备重试: attempt={}/{}, error={}",
                        event.getNumberOfRetryAttempts(), maxAttempts,
                        event.getLastThrowable().getMessage()));

        try {
            return Retry.decorateCallable(retry, task).call();
        } catch (Exception e) {
            log.error("任务执行失败，已达最大重试次数: maxAttempts={}", maxAttempts, e);
            throw wrapException(e);
        }
    }

    // ==================== 便捷方法 ====================

    public static <T> T retry(Callable<T> task) {
        return retry(task, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
    }

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
     */
    public static <T> T retryWithBackoff(Callable<T> task, int maxAttempts,
                                          long initialDelay, long maxDelay, double multiplier) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(initialDelay))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(initialDelay, multiplier))
                .build();

        Retry retry = Retry.of("retryBackoff-" + System.nanoTime(), config);

        retry.getEventPublisher().onRetry(event ->
                log.warn("任务执行失败，准备指数退避重试: attempt={}/{}, error={}",
                        event.getNumberOfRetryAttempts(), maxAttempts,
                        event.getLastThrowable().getMessage()));

        try {
            return Retry.decorateCallable(retry, task).call();
        } catch (Exception e) {
            log.error("任务执行失败，已达最大重试次数: maxAttempts={}", maxAttempts, e);
            throw wrapException(e);
        }
    }

    public static <T> T retryWithBackoff(Callable<T> task) {
        return retryWithBackoff(task, 3, 100, 5000, 2.0);
    }

    // ==================== 辅助方法 ====================

    private static RuntimeException wrapException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }
}
