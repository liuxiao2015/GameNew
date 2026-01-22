package com.game.core.context;

import com.game.core.net.session.Session;
import lombok.Getter;
import lombok.Setter;

/**
 * 请求上下文
 * <p>
 * 线程安全的请求上下文，存储当前请求相关信息
 * 开发者可直接获取，无需在方法参数中传递 Session
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @ProtocolMapping(methodId = 0x01, desc = "获取玩家信息")
 * public PlayerInfoResponse getPlayerInfo(GetPlayerInfoRequest request) {
 *     long roleId = RequestContext.getRoleId();  // 直接获取
 *     Session session = RequestContext.getSession();
 *     // 业务逻辑...
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
public class RequestContext {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    /**
     * 当前会话
     */
    @Getter
    @Setter
    private Session session;

    /**
     * 请求序列号
     */
    @Getter
    @Setter
    private int seqId;

    /**
     * 协议号
     */
    @Getter
    @Setter
    private int protocolKey;

    /**
     * 请求开始时间
     */
    @Getter
    private final long startTime;

    /**
     * 链路追踪ID
     */
    @Getter
    @Setter
    private String traceId;

    private RequestContext() {
        this.startTime = System.currentTimeMillis();
    }

    // ==================== 静态便捷方法 ====================

    /**
     * 初始化上下文
     */
    public static RequestContext init() {
        RequestContext context = new RequestContext();
        CONTEXT.set(context);
        return context;
    }

    /**
     * 获取当前上下文
     */
    public static RequestContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前会话
     */
    public static Session getSession() {
        RequestContext ctx = get();
        return ctx != null ? ctx.session : null;
    }

    /**
     * 获取当前账号ID
     */
    public static long getAccountId() {
        Session session = getSession();
        return session != null ? session.getAccountId() : 0;
    }

    /**
     * 获取当前角色ID
     */
    public static long getRoleId() {
        Session session = getSession();
        return session != null ? session.getRoleId() : 0;
    }

    /**
     * 获取当前角色名
     */
    public static String getRoleName() {
        Session session = getSession();
        return session != null ? session.getRoleName() : null;
    }

    /**
     * 获取当前服务器ID
     */
    public static int getServerId() {
        Session session = getSession();
        return session != null ? session.getServerId() : 0;
    }

    /**
     * 判断是否已登录
     */
    public static boolean isLoggedIn() {
        Session session = getSession();
        return session != null && session.isLoggedIn();
    }

    /**
     * 判断是否有角色
     */
    public static boolean hasRole() {
        Session session = getSession();
        return session != null && session.hasRole();
    }

    /**
     * 获取请求耗时 (毫秒)
     */
    public static long getCostTime() {
        RequestContext ctx = get();
        return ctx != null ? System.currentTimeMillis() - ctx.startTime : 0;
    }

    /**
     * 获取会话属性
     */
    public static <T> T getSessionAttribute(String key) {
        Session session = getSession();
        return session != null ? session.getAttribute(key) : null;
    }

    /**
     * 设置会话属性
     */
    public static void setSessionAttribute(String key, Object value) {
        Session session = getSession();
        if (session != null) {
            session.setAttribute(key, value);
        }
    }
}
