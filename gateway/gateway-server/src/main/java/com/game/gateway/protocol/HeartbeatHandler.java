package com.game.gateway.protocol;

import com.game.common.protocol.ProtocolConstants;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 心跳处理器
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatHandler {

    /**
     * 处理心跳
     */
    public GameMessage handle(Session session, GameMessage request) {
        session.updateActiveTime();
        
        GameMessage response = new GameMessage();
        response.setType(GameMessage.Type.RESPONSE);
        response.setSeqId(request.getSeqId());
        response.setErrorCode(0);
        response.setBody(new byte[0]);
        
        log.trace("心跳响应: sessionId={}", session.getSessionId());
        return response;
    }
}
