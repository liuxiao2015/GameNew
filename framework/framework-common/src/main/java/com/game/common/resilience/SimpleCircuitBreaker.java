package com.game.common.resilience;

import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 熔断器 (基于 Resilience4j)
 * <p>
 * 保持原有 API 不变，内部委托 Resilience4j CircuitBreaker。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * SimpleCircuitBreaker breaker = SimpleCircuitBreaker.of("playerService", 5, 30000);
 * Result<PlayerDTO> result = breaker.call(() -> playerService.getPlayer(roleId));
 *
 * PlayerDTO player = breaker.callWithFallback(
 *     () -> playerService.getPlayer(roleId),
 *     () -> new PlayerDTO()
 * );
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public class SimpleCircuitBreaker {

    private final CircuitBreaker circuitBreaker;
    private final String resourceName;

    /**
     * 全局注册表
     */
    private static final CircuitBreakerRegistry REGISTRY = CircuitBreakerRegistry.ofDefaults();

    private SimpleCircuitBreaker(String resourceName, int errorThreshold, long recoveryTimeMs) {
        this.resourceName = resourceName;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(errorThreshold)
                .slidingWindowSize(errorThreshold * 2)
                .waitDurationInOpenState(Duration.ofMillis(recoveryTimeMs))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        this.circuitBreaker = REGISTRY.circuitBreaker(resourceName, config);
    }

    /**
     * 获取或创建熔断器
     */
    public static SimpleCircuitBreaker of(String resourceName, int errorThreshold, long recoveryTimeMs) {
        return new SimpleCircuitBreaker(resourceName, errorThreshold, recoveryTimeMs);
    }

    /**
     * 获取或创建熔断器 (默认: 5次错误，30秒恢复)
     */
    public static SimpleCircuitBreaker of(String resourceName) {
        return of(resourceName, 5, 30000);
    }

    /**
     * 熔断状态
     */
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    /**
     * 获取当前状态
     */
    public State getState() {
        return switch (circuitBreaker.getState()) {
            case OPEN, FORCED_OPEN -> State.OPEN;
            case HALF_OPEN -> State.HALF_OPEN;
            default -> State.CLOSED;
        };
    }

    /**
     * 执行带熔断的调用
     */
    public <T> Result<T> call(Supplier<Result<T>> supplier) {
        try {
            Result<T> result = circuitBreaker.executeSupplier(supplier);
            if (!result.isSuccess()) {
                circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS,
                        new RuntimeException("业务失败: " + result));
            }
            return result;
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            log.warn("服务熔断中: resource={}", resourceName);
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "服务暂时不可用");
        } catch (Exception e) {
            log.error("服务调用异常: resource={}", resourceName, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 执行带熔断和降级的调用
     */
    public <T> T callWithFallback(Supplier<T> supplier, Supplier<T> fallback) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            log.warn("服务熔断中，执行降级: resource={}", resourceName);
            return fallback.get();
        } catch (Exception e) {
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
     * 手动重置熔断器
     */
    public void forceReset() {
        circuitBreaker.reset();
    }

    /**
     * 获取错误计数
     */
    public int getErrorCount() {
        return (int) circuitBreaker.getMetrics().getNumberOfFailedCalls();
    }

    /**
     * 是否熔断中
     */
    public boolean isTripped() {
        return getState() != State.CLOSED;
    }

    /**
     * 获取底层 Resilience4j CircuitBreaker (便于高级使用)
     */
    public CircuitBreaker getResilience4jCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public String toString() {
        return String.format("CircuitBreaker[%s, state=%s, errors=%d]",
                resourceName, getState(), getErrorCount());
    }
}
