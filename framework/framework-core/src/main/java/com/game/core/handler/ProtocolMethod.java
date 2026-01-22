package com.game.core.handler;

import com.game.core.handler.annotation.Protocol;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 协议处理方法信息
 * <p>
 * 封装协议处理方法的元数据，包括：
 * <ul>
 *     <li>协议号信息</li>
 *     <li>处理器实例和方法引用</li>
 *     <li>权限验证配置</li>
 *     <li>限流和性能监控</li>
 *     <li>Protobuf 解析器</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Getter
public class ProtocolMethod {

    // ==================== 协议标识 ====================

    /**
     * 模块 ID
     */
    private final int moduleId;

    /**
     * 方法 ID
     */
    private final int methodId;

    /**
     * 完整协议 Key (moduleId << 8 | methodId) 或直接指定的协议号
     */
    private final int protocolKey;

    /**
     * 协议描述
     */
    private final String description;

    // ==================== 处理器信息 ====================

    /**
     * 处理器实例
     */
    private final Object handler;

    /**
     * 处理方法
     */
    private final Method method;

    /**
     * 请求参数类型
     */
    private final Class<?> requestType;

    /**
     * 响应类型
     */
    private final Class<?> responseType;

    /**
     * Protobuf 解析器 (如果请求参数是 Protobuf 消息)
     */
    private final Parser<?> parser;

    // ==================== 权限配置 ====================

    /**
     * 是否需要登录
     */
    private final boolean requireLogin;

    /**
     * 是否需要角色
     */
    private final boolean requireRole;

    // ==================== 执行配置 ====================

    /**
     * 限流阈值 (每秒)
     */
    private final int rateLimit;

    /**
     * 慢请求阈值 (毫秒)
     */
    private final int slowThreshold;

    /**
     * 是否在 Actor 中执行
     */
    private final boolean executeInActor;

    /**
     * 是否异步执行
     */
    private final boolean async;

    // ==================== 统计数据 ====================

    /**
     * 当前秒请求计数
     */
    private final AtomicInteger currentSecondCount = new AtomicInteger(0);

    /**
     * 当前秒时间戳
     */
    private final AtomicLong currentSecond = new AtomicLong(0);

    /**
     * 总请求数
     */
    private final AtomicLong totalCount = new AtomicLong(0);

    /**
     * 总耗时 (纳秒)
     */
    private final AtomicLong totalCostNanos = new AtomicLong(0);

    /**
     * 最大耗时 (纳秒)
     */
    private final AtomicLong maxCostNanos = new AtomicLong(0);

    /**
     * 构造协议方法
     */
    public ProtocolMethod(Object handler, Method method, Protocol protocol,
                          int controllerModuleId, Class<?> requestType) {
        this.handler = handler;
        this.method = method;
        this.requestType = requestType;
        this.responseType = method.getReturnType();
        this.description = protocol.desc();
        this.requireLogin = protocol.requireLogin();
        this.requireRole = protocol.requireRole();
        this.rateLimit = protocol.rateLimit();
        this.slowThreshold = protocol.slowThreshold();
        this.executeInActor = protocol.executeInActor();
        this.async = protocol.async();

        // 计算协议号
        if (protocol.value() > 0) {
            // 使用完整协议号
            this.protocolKey = protocol.value();
            this.moduleId = (protocol.value() >> 8) & 0xFF;
            this.methodId = protocol.value() & 0xFF;
        } else {
            // 使用 moduleId + methodId 组合
            this.moduleId = controllerModuleId;
            this.methodId = protocol.methodId();
            this.protocolKey = (controllerModuleId << 8) | protocol.methodId();
        }

        // 获取 Protobuf Parser
        this.parser = initParser(requestType);

        // 允许反射调用
        method.setAccessible(true);
    }

    /**
     * 初始化 Protobuf Parser
     */
    @SuppressWarnings("unchecked")
    private Parser<?> initParser(Class<?> requestType) {
        if (requestType == null || !Message.class.isAssignableFrom(requestType)) {
            return null;
        }
        try {
            Method parserMethod = requestType.getMethod("parser");
            return (Parser<?>) parserMethod.invoke(null);
        } catch (Exception e) {
            log.warn("获取 Protobuf Parser 失败: class={}", requestType.getName());
            return null;
        }
    }

    /**
     * 调用处理方法
     */
    public Object invoke(Object... args) throws Exception {
        return method.invoke(handler, args);
    }

    /**
     * 检查是否被限流
     *
     * @return true=被限流，false=允许通过
     */
    public boolean isRateLimited() {
        if (rateLimit <= 0) {
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        long lastSecond = currentSecond.get();

        if (now != lastSecond) {
            // 新的一秒，重置计数
            if (currentSecond.compareAndSet(lastSecond, now)) {
                currentSecondCount.set(1);
                return false;
            }
        }

        // 检查是否超过限流
        return currentSecondCount.incrementAndGet() > rateLimit;
    }

    /**
     * 记录请求统计
     */
    public void recordStats(long costNanos) {
        totalCount.incrementAndGet();
        totalCostNanos.addAndGet(costNanos);
        // 更新最大耗时
        long currentMax = maxCostNanos.get();
        while (costNanos > currentMax) {
            if (maxCostNanos.compareAndSet(currentMax, costNanos)) {
                break;
            }
            currentMax = maxCostNanos.get();
        }
    }

    /**
     * 获取平均耗时 (毫秒)
     */
    public double getAvgCostMs() {
        long count = totalCount.get();
        if (count == 0) {
            return 0;
        }
        return totalCostNanos.get() / 1_000_000.0 / count;
    }

    /**
     * 获取最大耗时 (毫秒)
     */
    public double getMaxCostMs() {
        return maxCostNanos.get() / 1_000_000.0;
    }

    /**
     * 获取协议名称 (用于日志)
     */
    public String getName() {
        if (description != null && !description.isEmpty()) {
            return String.format("%s[%d]", description, protocolKey);
        }
        return String.format("Protocol[%d]", protocolKey);
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format("total=%d, avg=%.2fms, max=%.2fms",
                totalCount.get(), getAvgCostMs(), getMaxCostMs());
    }

    @Override
    public String toString() {
        return String.format("Protocol[%d] %s -> %s.%s",
                protocolKey, description,
                handler.getClass().getSimpleName(), method.getName());
    }
}
