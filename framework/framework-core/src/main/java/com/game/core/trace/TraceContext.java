package com.game.core.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文
 * <p>
 * 提供请求链路追踪能力：
 * <ul>
 *     <li>自动生成 traceId</li>
 *     <li>支持 MDC 日志输出</li>
 *     <li>跨服务传递</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 日志输出示例 (配合 logback pattern):
 * %d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] [%X{roleId}] %-5level - %msg%n
 *
 * 输出: 2026-01-23 10:00:00 [abc123] [12345678] INFO - 玩家登录成功
 * </pre>
 *
 * @author GameServer
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String ROLE_ID_KEY = "roleId";
    public static final String ACCOUNT_ID_KEY = "accountId";
    public static final String SERVER_ID_KEY = "serverId";

    private TraceContext() {}

    /**
     * 开始追踪 (自动生成 traceId)
     */
    public static String start() {
        String traceId = generateTraceId();
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 开始追踪 (指定 traceId)
     */
    public static void start(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 设置角色 ID
     */
    public static void setRoleId(long roleId) {
        if (roleId > 0) {
            MDC.put(ROLE_ID_KEY, String.valueOf(roleId));
        }
    }

    /**
     * 设置账号 ID
     */
    public static void setAccountId(long accountId) {
        if (accountId > 0) {
            MDC.put(ACCOUNT_ID_KEY, String.valueOf(accountId));
        }
    }

    /**
     * 设置服务器 ID
     */
    public static void setServerId(int serverId) {
        if (serverId > 0) {
            MDC.put(SERVER_ID_KEY, String.valueOf(serverId));
        }
    }

    /**
     * 获取当前 traceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 获取当前 roleId
     */
    public static String getRoleId() {
        return MDC.get(ROLE_ID_KEY);
    }

    /**
     * 结束追踪
     */
    public static void end() {
        MDC.clear();
    }

    /**
     * 生成 traceId
     * <p>
     * 格式: 8位随机字符 + 4位时间戳后缀
     * </p>
     */
    private static String generateTraceId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timePart = Long.toString(System.currentTimeMillis() % 10000, 36);
        return uuid.substring(0, 8) + timePart;
    }

    /**
     * 在当前上下文中执行任务
     */
    public static void run(Runnable task) {
        String traceId = getTraceId();
        try {
            task.run();
        } finally {
            // 恢复原 traceId（用于线程池场景）
            if (traceId != null) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
        }
    }

    /**
     * 复制当前上下文
     */
    public static java.util.Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 设置上下文
     */
    public static void setContextMap(java.util.Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}
