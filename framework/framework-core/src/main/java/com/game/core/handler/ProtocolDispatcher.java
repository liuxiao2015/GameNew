package com.game.core.handler;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.game.core.context.RequestContext;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.trace.TraceContext;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一协议分发器
 * <p>
 * 自动分发协议到对应的处理方法，支持：
 * <ul>
 *     <li>自动参数注入 (Session、请求消息、roleId等)</li>
 *     <li>登录/角色验证</li>
 *     <li>请求限流</li>
 *     <li>业务异常自动转换</li>
 *     <li>慢请求告警</li>
 *     <li>请求上下文管理</li>
 *     <li>异步执行支持</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolDispatcher {

    private final ProtocolHandlerRegistry registry;
    private final com.game.core.monitor.ServerMonitor serverMonitor;
    private final com.game.core.alert.AlertService alertService;

    /**
     * 异步执行线程池 (使用虚拟线程)
     */
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 分发协议消息
     *
     * @param session 会话
     * @param message 游戏消息
     * @return 响应消息 (可为 null 表示无需响应)
     */
    public GameMessage dispatch(Session session, GameMessage message) {
        int protocolKey = message.getProtocolId();
        long startNanos = System.nanoTime();

        // 初始化链路追踪
        String traceId = TraceContext.start();
        TraceContext.setRoleId(session.getRoleId());
        TraceContext.setAccountId(session.getAccountId());
        TraceContext.setServerId(session.getServerId());

        // 初始化请求上下文
        RequestContext context = RequestContext.init();
        context.setSession(session);
        context.setSeqId(message.getSeqId());
        context.setProtocolKey(protocolKey);
        context.setTraceId(traceId);

        // 标记是否异步执行（异步时主线程不清理上下文，由异步线程负责）
        boolean isAsync = false;

        try {
            // 1. 查找处理方法
            ProtocolMethod protocolMethod = registry.getProtocolMethod(protocolKey);
            if (protocolMethod == null) {
                log.warn("未找到协议处理器: protocolKey={}, session={}",
                        protocolKey, session.getSessionId());
                return createErrorResponse(message, ErrorCode.ILLEGAL_OPERATION);
            }

            // 2. 登录验证
            if (protocolMethod.isRequireLogin() && !session.isLoggedIn()) {
                log.warn("未登录访问需要登录的协议: {}, session={}",
                        protocolMethod.getName(), session.getSessionId());
                return createErrorResponse(message, ErrorCode.UNAUTHORIZED);
            }

            // 3. 角色验证
            if (protocolMethod.isRequireRole() && session.getRoleId() <= 0) {
                log.warn("未选择角色访问需要角色的协议: {}, session={}",
                        protocolMethod.getName(), session.getSessionId());
                return createErrorResponse(message, ErrorCode.ROLE_NOT_FOUND);
            }

            // 4. 限流检查
            if (protocolMethod.isRateLimited()) {
                log.warn("请求被限流: {}, session={}, roleId={}",
                        protocolMethod.getName(), session.getSessionId(), session.getRoleId());
                return createErrorResponse(message, ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            // 5. 异步执行
            if (protocolMethod.isAsync()) {
                // 标记为异步，主线程不清理上下文
                isAsync = true;
                
                // 复制当前追踪上下文
                Map<String, String> traceContextMap = TraceContext.getCopyOfContextMap();
                
                asyncExecutor.execute(() -> {
                    try {
                        // 恢复追踪上下文
                        TraceContext.setContextMap(traceContextMap);
                        
                        RequestContext asyncContext = RequestContext.init();
                        asyncContext.setSession(session);
                        asyncContext.setSeqId(message.getSeqId());
                        asyncContext.setProtocolKey(protocolKey);
                        asyncContext.setTraceId(traceId);
                        
                        GameMessage response = executeHandler(session, message, protocolMethod, startNanos);
                        if (response != null) {
                            session.send(response);
                        }
                    } finally {
                        // 异步线程负责清理上下文
                        RequestContext.clear();
                        TraceContext.end();
                    }
                });
                return null; // 异步不立即返回
            }

            // 6. 同步执行
            return executeHandler(session, message, protocolMethod, startNanos);

        } catch (BizException e) {
            // 业务异常 - 正常的业务错误，不打印堆栈
            log.info("业务异常: protocolKey={}, code={}, message={}, roleId={}",
                    protocolKey, e.getCode(), e.getMessage(), session.getRoleId());
            serverMonitor.recordRequest(false);
            return createErrorResponse(message, e.getCode(), e.getMessage());

        } catch (Exception e) {
            // 系统异常 - 需要告警
            Throwable cause = e.getCause() != null ? e.getCause() : e;

            // 如果根因是业务异常，按业务异常处理
            if (cause instanceof BizException bizEx) {
                log.info("业务异常: protocolKey={}, code={}, message={}, roleId={}",
                        protocolKey, bizEx.getCode(), bizEx.getMessage(), session.getRoleId());
                serverMonitor.recordRequest(false);
                return createErrorResponse(message, bizEx.getCode(), bizEx.getMessage());
            }

            log.error("协议处理异常: protocolKey={}, session={}, roleId={}",
                    protocolKey, session.getSessionId(), session.getRoleId(), cause);
            
            // 发送告警
            alertService.alertException("协议处理异常: " + protocolKey, cause);
            serverMonitor.recordRequest(false);
            
            return createErrorResponse(message, ErrorCode.SYSTEM_ERROR);

        } finally {
            // 只有同步执行时才在主线程清理上下文
            // 异步执行由异步线程负责清理
            if (!isAsync) {
                RequestContext.clear();
                TraceContext.end();
            }
        }
    }

    /**
     * 执行协议处理器
     */
    private GameMessage executeHandler(Session session, GameMessage message,
                                        ProtocolMethod protocolMethod, long startNanos) {
        try {
            // 1. 构建参数
            Object[] args = buildArgs(protocolMethod, session, message);

            // 2. 调用处理方法
            Object result = protocolMethod.invoke(args);

            // 3. 统计
            long costNanos = System.nanoTime() - startNanos;
            protocolMethod.recordStats(costNanos);

            // 4. 慢请求告警
            long costMs = costNanos / 1_000_000;
            if (costMs > protocolMethod.getSlowThreshold()) {
                log.warn("慢请求: {} cost={}ms, session={}, roleId={}",
                        protocolMethod.getName(), costMs, session.getSessionId(), session.getRoleId());
                alertService.alertPerformance(protocolMethod.getName(), costMs, protocolMethod.getSlowThreshold());
            }

            // 5. 记录成功
            serverMonitor.recordRequest(true);

            // 6. 构建响应
            return createResponse(message, result);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建方法参数
     */
    private Object[] buildArgs(ProtocolMethod protocolMethod, Session session, GameMessage message) {
        Parameter[] parameters = protocolMethod.getMethod().getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            String paramName = parameters[i].getName();

            if (Session.class.isAssignableFrom(paramType)) {
                // 注入 Session
                args[i] = session;
            } else if (GameMessage.class.isAssignableFrom(paramType)) {
                // 注入原始消息
                args[i] = message;
            } else if (RequestContext.class.isAssignableFrom(paramType)) {
                // 注入请求上下文
                args[i] = RequestContext.get();
            } else if (Message.class.isAssignableFrom(paramType)) {
                // 注入 Protobuf 请求消息
                args[i] = parseRequest(message, protocolMethod);
            } else if (paramType == long.class) {
                // 注入 long 类型参数
                args[i] = resolveLongParam(paramName, session);
            } else if (paramType == int.class) {
                // 注入 int 类型参数
                args[i] = resolveIntParam(paramName, session, message);
            } else {
                // 默认尝试解析为请求消息
                args[i] = parseRequest(message, protocolMethod);
            }
        }

        return args;
    }

    /**
     * 解析 long 类型参数
     */
    private long resolveLongParam(String paramName, Session session) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("roleid")) {
            return session.getRoleId();
        } else if (lowerName.contains("accountid")) {
            return session.getAccountId();
        }
        return 0L;
    }

    /**
     * 解析 int 类型参数
     */
    private int resolveIntParam(String paramName, Session session, GameMessage message) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("seqid")) {
            return message.getSeqId();
        } else if (lowerName.contains("serverid")) {
            return session.getServerId();
        }
        return 0;
    }

    /**
     * 解析请求消息
     */
    private Object parseRequest(GameMessage message, ProtocolMethod protocolMethod) {
        try {
            if (protocolMethod.getParser() != null && message.getBody() != null) {
                return protocolMethod.getParser().parseFrom(message.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("解析请求消息失败: protocol={}", protocolMethod.getName(), e);
            return null;
        }
    }

    /**
     * 创建响应消息
     */
    private GameMessage createResponse(GameMessage request, Object result) {
        if (result == null) {
            // 返回 null 表示成功但无数据
            return GameMessage.createResponse(
                    request.getSeqId(),
                    request.getProtocolId(),
                    0,
                    ErrorCode.SUCCESS.getCode(),
                    null
            );
        }

        if (result instanceof GameMessage gameMessage) {
            return gameMessage;
        }

        if (result instanceof Message protoMessage) {
            return GameMessage.createResponse(
                    request.getSeqId(),
                    request.getProtocolId(),
                    0,
                    ErrorCode.SUCCESS.getCode(),
                    protoMessage
            );
        }

        // 其他类型暂不处理
        return null;
    }

    /**
     * 创建错误响应
     */
    private GameMessage createErrorResponse(GameMessage request, ErrorCode errorCode) {
        return GameMessage.createResponse(
                request.getSeqId(),
                request.getProtocolId(),
                0,
                errorCode.getCode(),
                null
        );
    }

    /**
     * 创建错误响应 (自定义错误码)
     */
    private GameMessage createErrorResponse(GameMessage request, int errorCode, String msg) {
        return GameMessage.createResponse(
                request.getSeqId(),
                request.getProtocolId(),
                0,
                errorCode,
                null
        );
    }
}
