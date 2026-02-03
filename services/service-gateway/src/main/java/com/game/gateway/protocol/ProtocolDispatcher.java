package com.game.gateway.protocol;

import com.game.common.enums.ErrorCode;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 协议分发器
 *
 * @author GameServer
 */
@Slf4j
@Component
public class ProtocolDispatcher {

    /**
     * 协议处理器映射
     */
    private final Map<Integer, ProtocolHandler<?>> handlers = new ConcurrentHashMap<>();

    /**
     * 注册协议处理器
     */
    public <T extends Message> void register(int protocolKey, ProtocolHandler<T> handler) {
        handlers.put(protocolKey, handler);
        log.debug("注册协议处理器: protocolKey={}", protocolKey);
    }

    /**
     * 分发协议
     */
    @SuppressWarnings("unchecked")
    public void dispatch(Session session, GameMessage message) {
        int protocolKey = message.getProtocolKey();
        
        ProtocolHandler<Message> handler = (ProtocolHandler<Message>) handlers.get(protocolKey);
        if (handler == null) {
            log.warn("未找到协议处理器: protocolKey={}", protocolKey);
            session.sendResponse(message.getSeqId(), ErrorCode.ILLEGAL_OPERATION.getCode(), null);
            return;
        }

        try {
            // 解析请求
            Message request = handler.parseRequest(message.getBody());
            
            // 处理请求
            Message response = handler.handle(session, request);
            
            // 发送响应
            byte[] responseData = response != null ? response.toByteArray() : new byte[0];
            session.sendResponse(message.getSeqId(), ErrorCode.SUCCESS.getCode(), responseData);
            
        } catch (Exception e) {
            log.error("协议处理异常: protocolKey={}, sessionId={}", protocolKey, session.getSessionId(), e);
            session.sendResponse(message.getSeqId(), ErrorCode.SYSTEM_ERROR.getCode(), null);
        }
    }

    /**
     * 协议处理器接口
     */
    public interface ProtocolHandler<T extends Message> {
        
        /**
         * 解析请求
         */
        T parseRequest(byte[] data) throws Exception;
        
        /**
         * 处理请求
         */
        Message handle(Session session, T request) throws Exception;
    }

    /**
     * 简单协议处理器
     */
    public static abstract class SimpleHandler<REQ extends Message, RESP extends Message> 
            implements ProtocolHandler<REQ> {
        
        private final com.google.protobuf.Parser<REQ> parser;
        
        protected SimpleHandler(com.google.protobuf.Parser<REQ> parser) {
            this.parser = parser;
        }
        
        @Override
        public REQ parseRequest(byte[] data) throws Exception {
            return parser.parseFrom(data);
        }
        
        @Override
        public Message handle(Session session, REQ request) throws Exception {
            return process(session, request);
        }
        
        /**
         * 处理请求
         */
        protected abstract RESP process(Session session, REQ request) throws Exception;
    }
}
