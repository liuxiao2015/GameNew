package com.game.core.net.handler;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;
import com.game.common.protocol.Protocol;
import com.game.common.protocol.ProtocolConstants;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息分发器
 * <p>
 * 扫描所有带有 @Protocol 注解的方法，自动注册协议处理器
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {

    private final ApplicationContext applicationContext;

    /**
     * 协议处理器映射
     */
    private final Map<Integer, ProtocolHandler> handlers = new ConcurrentHashMap<>();

    /**
     * 异步处理线程池
     */
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Session 关闭监听器
     */
    private final Map<String, SessionCloseListener> closeListeners = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scanProtocolHandlers();
        log.info("协议处理器扫描完成，共注册 {} 个协议", handlers.size());
    }

    /**
     * 扫描并注册协议处理器
     */
    private void scanProtocolHandlers() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        
        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                Protocol protocol = method.getAnnotation(Protocol.class);
                if (protocol != null) {
                    registerHandler(protocol, bean, method);
                }
            }
        }
    }

    /**
     * 注册协议处理器
     */
    private void registerHandler(Protocol protocol, Object bean, Method method) {
        int protocolId = protocol.value();
        
        if (handlers.containsKey(protocolId)) {
            log.warn("协议号重复: protocolId={}, method={}", protocolId, method);
            return;
        }

        // 解析参数类型，获取 Protobuf Parser
        Parser<?> parser = null;
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            if (Message.class.isAssignableFrom(paramType)) {
                try {
                    Method parserMethod = paramType.getMethod("parser");
                    parser = (Parser<?>) parserMethod.invoke(null);
                } catch (Exception e) {
                    log.error("获取 Protobuf Parser 失败: class={}", paramType, e);
                }
                break;
            }
        }

        ProtocolHandler handler = new ProtocolHandler(
                protocolId,
                protocol.desc(),
                protocol.requireLogin(),
                protocol.async(),
                bean,
                method,
                parser
        );

        handlers.put(protocolId, handler);
        log.debug("注册协议处理器: protocolId={}, desc={}, method={}.{}", 
                protocolId, protocol.desc(), bean.getClass().getSimpleName(), method.getName());
    }

    /**
     * 分发消息
     */
    public void dispatch(Session session, GameMessage message) {
        int protocolId = message.getProtocolId();
        
        ProtocolHandler handler = handlers.get(protocolId);
        if (handler == null) {
            log.warn("未找到协议处理器: protocolId={}", protocolId);
            session.sendResponse(message.getSeqId(), ErrorCode.ILLEGAL_OPERATION.getCode(), null);
            return;
        }

        // 登录检查
        if (handler.requireLogin() && !session.isAuthenticated()) {
            if (!ProtocolConstants.isLoginProtocol(protocolId)) {
                log.warn("未登录访问: sessionId={}, protocolId={}", session.getSessionId(), protocolId);
                session.sendResponse(message.getSeqId(), ErrorCode.TOKEN_INVALID.getCode(), null);
                return;
            }
        }

        // 执行处理
        if (handler.async()) {
            asyncExecutor.execute(() -> executeHandler(session, message, handler));
        } else {
            executeHandler(session, message, handler);
        }
    }

    /**
     * 执行协议处理器
     */
    private void executeHandler(Session session, GameMessage message, ProtocolHandler handler) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析请求参数
            Object request = null;
            if (handler.parser() != null && message.getBody() != null && message.getBody().length > 0) {
                request = handler.parser().parseFrom(message.getBody());
            }

            // 调用处理方法
            Object result = invokeHandler(handler, session, message, request);

            // 发送响应
            if (result instanceof Message protoResult) {
                session.sendResponse(message.getSeqId(), ErrorCode.SUCCESS.getCode(), protoResult.toByteArray());
            } else {
                session.sendResponse(message.getSeqId(), ErrorCode.SUCCESS.getCode(), null);
            }

        } catch (GameException e) {
            log.warn("业务异常: protocolId={}, errorCode={}, message={}", 
                    handler.protocolId(), e.getCode(), e.getMessage());
            session.sendResponse(message.getSeqId(), e.getCode(), null);

        } catch (Exception e) {
            log.error("协议处理异常: protocolId={}", handler.protocolId(), e);
            session.sendResponse(message.getSeqId(), ErrorCode.SYSTEM_ERROR.getCode(), null);

        } finally {
            long cost = System.currentTimeMillis() - startTime;
            if (cost > 100) {
                log.warn("协议处理耗时过长: protocolId={}, cost={}ms", handler.protocolId(), cost);
            }
        }
    }

    /**
     * 调用处理方法
     */
    private Object invokeHandler(ProtocolHandler handler, Session session, 
                                  GameMessage message, Object request) throws Exception {
        Method method = handler.method();
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (Session.class.isAssignableFrom(paramType)) {
                args[i] = session;
            } else if (GameMessage.class.isAssignableFrom(paramType)) {
                args[i] = message;
            } else if (Message.class.isAssignableFrom(paramType)) {
                args[i] = request;
            }
        }

        return method.invoke(handler.bean(), args);
    }

    /**
     * 注册 Session 关闭监听器
     */
    public void registerCloseListener(String name, SessionCloseListener listener) {
        closeListeners.put(name, listener);
    }

    /**
     * Session 关闭事件
     */
    public void onSessionClose(Session session) {
        for (SessionCloseListener listener : closeListeners.values()) {
            try {
                listener.onClose(session);
            } catch (Exception e) {
                log.error("Session 关闭监听器执行异常", e);
            }
        }
    }

    /**
     * 协议处理器记录
     */
    private record ProtocolHandler(
            int protocolId,
            String desc,
            boolean requireLogin,
            boolean async,
            Object bean,
            Method method,
            Parser<?> parser
    ) {}

    /**
     * Session 关闭监听器
     */
    @FunctionalInterface
    public interface SessionCloseListener {
        void onClose(Session session);
    }
}
