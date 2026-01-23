package com.game.core.async;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 异步任务服务
 * <p>
 * 提供统一的异步任务执行能力：
 * <ul>
 *     <li>使用 Java 21 虚拟线程，高并发低开销</li>
 *     <li>支持超时控制</li>
 *     <li>支持回调处理</li>
 *     <li>统一异常处理</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 简单异步执行
 * asyncService.run(() -> doSomething());
 *
 * // 异步执行并获取结果
 * CompletableFuture<Result> future = asyncService.supply(() -> queryData());
 *
 * // 带超时的异步执行
 * asyncService.runWithTimeout(() -> slowOperation(), 5, TimeUnit.SECONDS);
 *
 * // 异步执行并回调
 * asyncService.runThenAccept(() -> fetchData(), data -> processData(data));
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class AsyncService {

    /**
     * 虚拟线程执行器
     */
    private ExecutorService executor;

    /**
     * 调度器 (用于超时控制)
     */
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "async-scheduler");
            t.setDaemon(true);
            return t;
        });
        log.info("AsyncService 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        log.info("AsyncService 已关闭");
    }

    // ==================== 基础异步 ====================

    /**
     * 异步执行任务 (无返回值)
     */
    public CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(wrapTask(task), executor);
    }

    /**
     * 异步执行任务 (有返回值)
     */
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier), executor);
    }

    /**
     * 提交任务
     */
    public Future<?> submit(Runnable task) {
        return executor.submit(wrapTask(task));
    }

    /**
     * 提交任务 (有返回值)
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(wrapCallable(task));
    }

    // ==================== 带超时的异步 ====================

    /**
     * 异步执行并带超时控制
     */
    public CompletableFuture<Void> runWithTimeout(Runnable task, long timeout, TimeUnit unit) {
        return run(task).orTimeout(timeout, unit);
    }

    /**
     * 异步执行并带超时控制 (有返回值)
     */
    public <T> CompletableFuture<T> supplyWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return supply(supplier).orTimeout(timeout, unit);
    }

    // ==================== 带回调的异步 ====================

    /**
     * 异步执行并在完成后回调
     */
    public <T> CompletableFuture<Void> runThenAccept(Supplier<T> supplier, Consumer<T> callback) {
        return supply(supplier).thenAcceptAsync(callback, executor);
    }

    /**
     * 异步执行并在完成后执行另一个任务
     */
    public CompletableFuture<Void> runThenRun(Runnable first, Runnable second) {
        return run(first).thenRunAsync(second, executor);
    }

    // ==================== 延迟执行 ====================

    /**
     * 延迟执行任务
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(wrapTask(task), delay, unit);
    }

    /**
     * 延迟执行任务 (有返回值)
     */
    public <T> ScheduledFuture<T> schedule(Callable<T> task, long delay, TimeUnit unit) {
        return scheduler.schedule(wrapCallable(task), delay, unit);
    }

    /**
     * 周期执行任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(wrapTask(task), initialDelay, period, unit);
    }

    // ==================== 并行执行 ====================

    /**
     * 并行执行多个任务，等待全部完成
     */
    @SafeVarargs
    public final CompletableFuture<Void> runAll(Runnable... tasks) {
        CompletableFuture<?>[] futures = new CompletableFuture[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = run(tasks[i]);
        }
        return CompletableFuture.allOf(futures);
    }

    /**
     * 并行执行多个任务，任意一个完成即返回
     */
    @SafeVarargs
    public final <T> CompletableFuture<T> supplyAny(Supplier<T>... suppliers) {
        CompletableFuture<T>[] futures = new CompletableFuture[suppliers.length];
        for (int i = 0; i < suppliers.length; i++) {
            futures[i] = supply(suppliers[i]);
        }
        return CompletableFuture.anyOf(futures).thenApply(o -> (T) o);
    }

    // ==================== 异常处理包装 ====================

    private Runnable wrapTask(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("异步任务执行异常", e);
                throw e;
            }
        };
    }

    private <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error("异步任务执行异常", e);
                throw e;
            }
        };
    }

    private <T> Callable<T> wrapCallable(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                log.error("异步任务执行异常", e);
                throw e;
            }
        };
    }
}
