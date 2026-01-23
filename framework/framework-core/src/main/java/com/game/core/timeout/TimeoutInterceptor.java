package com.game.core.timeout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 请求超时拦截器
 * <p>
 * 防止请求处理时间过长导致资源耗尽
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class TimeoutInterceptor {

    @Value("${game.request.timeout-ms:5000}")
    private long defaultTimeoutMs;

    /**
     * 超时执行器
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 带超时执行任务
     *
     * @param task      任务
     * @param timeoutMs 超时时间 (毫秒)
     * @param <T>       返回类型
     * @return 执行结果
     * @throws TimeoutException 超时异常
     */
    public <T> T executeWithTimeout(Callable<T> task, long timeoutMs) throws TimeoutException {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("请求处理超时: timeout={}ms", timeoutMs);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("请求被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException rte) {
                throw rte;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * 使用默认超时执行
     */
    public <T> T executeWithTimeout(Callable<T> task) throws TimeoutException {
        return executeWithTimeout(task, defaultTimeoutMs);
    }

    /**
     * 带超时执行 (无返回值)
     */
    public void runWithTimeout(Runnable task, long timeoutMs) throws TimeoutException {
        executeWithTimeout(() -> {
            task.run();
            return null;
        }, timeoutMs);
    }

    /**
     * 带超时执行 (返回 Optional)
     */
    public <T> java.util.Optional<T> tryExecuteWithTimeout(Callable<T> task, long timeoutMs) {
        try {
            return java.util.Optional.ofNullable(executeWithTimeout(task, timeoutMs));
        } catch (TimeoutException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * 带超时和默认值执行
     */
    public <T> T executeWithTimeoutOrDefault(Callable<T> task, long timeoutMs, T defaultValue) {
        try {
            return executeWithTimeout(task, timeoutMs);
        } catch (TimeoutException e) {
            return defaultValue;
        }
    }
}
