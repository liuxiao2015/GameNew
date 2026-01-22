package com.game.common.resilience;

import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 简易熔断器
 * <p>
 * 适合小团队使用的轻量级熔断实现，无需额外依赖：
 * <ul>
 *     <li>基于错误次数和错误率的熔断</li>
 *     <li>自动恢复机制</li>
 *     <li>支持降级逻辑</li>
 * </ul>
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 创建熔断器
 * SimpleCircuitBreaker breaker = SimpleCircuitBreaker.of("playerService", 5, 30000);
 * 
 * // 执行带熔断的调用
 * Result<PlayerDTO> result = breaker.call(() -> playerService.getPlayer(roleId));
 * 
 * // 带降级的调用
 * PlayerDTO player = breaker.callWithFallback(
 *     () -> playerService.getPlayer(roleId),
 *     () -> new PlayerDTO() // 降级返回默认值
 * );
 * }</pre>
 *
 * @author GameServer
 */
@Slf4j
public class SimpleCircuitBreaker {

    /**
     * 熔断器缓存
     */
    private static final Map<String, SimpleCircuitBreaker> BREAKERS = new ConcurrentHashMap<>();

    /**
     * 资源名称
     */
    private final String resourceName;

    /**
     * 错误阈值 (连续错误多少次后熔断)
     */
    private final int errorThreshold;

    /**
     * 熔断恢复时间 (毫秒)
     */
    private final long recoveryTimeMs;

    /**
     * 当前错误计数
     */
    private final AtomicInteger errorCount = new AtomicInteger(0);

    /**
     * 熔断开始时间
     */
    private final AtomicLong tripTime = new AtomicLong(0);

    /**
     * 熔断状态
     */
    public enum State {
        CLOSED,      // 正常
        OPEN,        // 熔断
        HALF_OPEN    // 半开 (尝试恢复)
    }

    private SimpleCircuitBreaker(String resourceName, int errorThreshold, long recoveryTimeMs) {
        this.resourceName = resourceName;
        this.errorThreshold = errorThreshold;
        this.recoveryTimeMs = recoveryTimeMs;
    }

    /**
     * 获取或创建熔断器
     *
     * @param resourceName   资源名称
     * @param errorThreshold 错误阈值
     * @param recoveryTimeMs 恢复时间 (毫秒)
     */
    public static SimpleCircuitBreaker of(String resourceName, int errorThreshold, long recoveryTimeMs) {
        return BREAKERS.computeIfAbsent(resourceName, 
            k -> new SimpleCircuitBreaker(resourceName, errorThreshold, recoveryTimeMs));
    }

    /**
     * 获取或创建熔断器 (默认配置: 5次错误，30秒恢复)
     */
    public static SimpleCircuitBreaker of(String resourceName) {
        return of(resourceName, 5, 30000);
    }

    /**
     * 获取当前状态
     */
    public State getState() {
        long trip = tripTime.get();
        if (trip == 0) {
            return State.CLOSED;
        }
        
        long elapsed = System.currentTimeMillis() - trip;
        if (elapsed >= recoveryTimeMs) {
            return State.HALF_OPEN;
        }
        return State.OPEN;
    }

    /**
     * 执行带熔断的调用
     */
    public <T> Result<T> call(Supplier<Result<T>> supplier) {
        State state = getState();
        
        // 熔断状态
        if (state == State.OPEN) {
            log.warn("服务熔断中: resource={}", resourceName);
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "服务暂时不可用");
        }

        try {
            Result<T> result = supplier.get();
            
            // 成功，重置错误计数
            if (result.isSuccess()) {
                reset();
            } else {
                recordError();
            }
            
            return result;
            
        } catch (Exception e) {
            recordError();
            log.error("服务调用异常: resource={}", resourceName, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 执行带熔断和降级的调用
     */
    public <T> T callWithFallback(Supplier<T> supplier, Supplier<T> fallback) {
        State state = getState();
        
        // 熔断状态，直接走降级
        if (state == State.OPEN) {
            log.warn("服务熔断中，执行降级: resource={}", resourceName);
            return fallback.get();
        }

        try {
            T result = supplier.get();
            reset();
            return result;
            
        } catch (Exception e) {
            recordError();
            log.error("服务调用异常，执行降级: resource={}", resourceName, e);
            return fallback.get();
        }
    }

    /**
     * 执行带熔断的调用，失败返回 null
     */
    public <T> T callOrNull(Supplier<T> supplier) {
        return callWithFallback(supplier, () -> null);
    }

    /**
     * 记录错误
     */
    private void recordError() {
        int count = errorCount.incrementAndGet();
        if (count >= errorThreshold) {
            trip();
        }
    }

    /**
     * 触发熔断
     */
    private void trip() {
        if (tripTime.get() == 0) {
            tripTime.set(System.currentTimeMillis());
            log.warn("服务熔断触发: resource={}, errorCount={}", resourceName, errorCount.get());
        }
    }

    /**
     * 重置熔断器
     */
    private void reset() {
        if (errorCount.get() > 0 || tripTime.get() > 0) {
            errorCount.set(0);
            tripTime.set(0);
            log.info("服务恢复正常: resource={}", resourceName);
        }
    }

    /**
     * 手动重置熔断器
     */
    public void forceReset() {
        reset();
    }

    /**
     * 获取错误计数
     */
    public int getErrorCount() {
        return errorCount.get();
    }

    /**
     * 是否熔断中
     */
    public boolean isTripped() {
        return getState() != State.CLOSED;
    }

    @Override
    public String toString() {
        return String.format("CircuitBreaker[%s, state=%s, errors=%d]",
                resourceName, getState(), errorCount.get());
    }
}
