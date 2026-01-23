package com.game.core.rpc.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.UUID;

/**
 * Dubbo 链路追踪 Filter
 * <p>
 * 在 RPC 调用链路中传递 TraceId，用于日志追踪和问题排查。
 * 自动激活于 Provider 和 Consumer 端。
 * </p>
 *
 * <pre>
 * 调用链路示例：
 * Gateway → GameService → GuildService → RankService
 *    │           │              │             │
 *    └───────────┴──────────────┴─────────────┘
 *                 共享同一个 TraceId
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class TraceFilter implements Filter {

    /**
     * TraceId 在 RPC 上下文中的 Key
     */
    public static final String TRACE_ID_KEY = "traceId";
    
    /**
     * 调用开始时间 Key
     */
    public static final String START_TIME_KEY = "startTime";

    /**
     * 当前线程的 TraceId
     */
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTime = System.currentTimeMillis();
        String traceId;
        boolean isProvider = RpcContext.getContext().isProviderSide();

        if (isProvider) {
            // Provider 端: 从上游获取 TraceId，如果没有则生成新的
            traceId = RpcContext.getContext().getAttachment(TRACE_ID_KEY);
            if (traceId == null || traceId.isEmpty()) {
                traceId = generateTraceId();
            }
            TRACE_ID_HOLDER.set(traceId);
        } else {
            // Consumer 端: 获取当前 TraceId 或生成新的，传递给下游
            traceId = TRACE_ID_HOLDER.get();
            if (traceId == null || traceId.isEmpty()) {
                traceId = generateTraceId();
                TRACE_ID_HOLDER.set(traceId);
            }
            RpcContext.getContext().setAttachment(TRACE_ID_KEY, traceId);
        }

        // 设置到 MDC，用于日志输出
        org.slf4j.MDC.put(TRACE_ID_KEY, traceId);

        try {
            // 执行调用
            Result result = invoker.invoke(invocation);

            // 记录调用耗时
            long cost = System.currentTimeMillis() - startTime;
            String serviceName = invoker.getInterface().getSimpleName();
            String methodName = invocation.getMethodName();

            if (cost > 100) {
                // 慢调用警告
                log.warn("[Trace] 慢调用: {}.{} traceId={} cost={}ms", 
                        serviceName, methodName, traceId, cost);
            } else if (log.isDebugEnabled()) {
                log.debug("[Trace] {}.{} traceId={} cost={}ms", 
                        serviceName, methodName, traceId, cost);
            }

            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[Trace] 调用异常: {}.{} traceId={} cost={}ms error={}", 
                    invoker.getInterface().getSimpleName(), 
                    invocation.getMethodName(), 
                    traceId, cost, e.getMessage());
            throw e;
        } finally {
            // Provider 端清理
            if (isProvider) {
                TRACE_ID_HOLDER.remove();
            }
            org.slf4j.MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 生成 TraceId
     * <p>
     * 格式: 时间戳(13位) + 随机数(8位)
     * </p>
     */
    private String generateTraceId() {
        return Long.toHexString(System.currentTimeMillis()) + 
               UUID.randomUUID().toString().substring(0, 8).replace("-", "");
    }

    /**
     * 获取当前 TraceId
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 设置 TraceId（用于非 RPC 入口，如 HTTP 请求）
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
        org.slf4j.MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 清理 TraceId
     */
    public static void clearTraceId() {
        TRACE_ID_HOLDER.remove();
        org.slf4j.MDC.remove(TRACE_ID_KEY);
    }
}
