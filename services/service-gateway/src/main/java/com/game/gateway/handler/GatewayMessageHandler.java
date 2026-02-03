package com.game.gateway.handler;

import com.game.common.protocol.Protocol;
import com.game.common.protocol.ProtocolConstants;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import com.game.proto.*;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网关消息处理器
 * <p>
 * 网关只处理握手和心跳，其他协议通过 ProtocolDispatcher 统一转发到后端服务
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMessageHandler {

    private final SessionManager sessionManager;

    /**
     * 处理握手请求
     */
    @Protocol(value = 0x0101, desc = "握手", requireLogin = false)
    public S2C_Handshake handleHandshake(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        C2S_Handshake request = C2S_Handshake.parseFrom(message.getBody());
        
        log.info("收到握手请求: sessionId={}, version={}, platform={}", 
                session.getSessionId(), request.getClientVersion(), request.getPlatform());

        // 保存设备信息到 Session
        session.setAttribute("deviceId", request.getDeviceId());
        session.setAttribute("platform", request.getPlatform());
        session.setAttribute("clientVersion", request.getClientVersion());

        return S2C_Handshake.newBuilder()
                .setResult(buildSuccessResult())
                .setServerTime(System.currentTimeMillis())
                .setSessionKey(session.getReconnectToken())
                .setNeedUpdate(false)
                .setNotice("")
                .build();
    }

    /**
     * 处理心跳请求
     */
    @Protocol(value = ProtocolConstants.HEARTBEAT_REQ, desc = "心跳", requireLogin = false)
    public S2C_Heartbeat handleHeartbeat(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        C2S_Heartbeat request = C2S_Heartbeat.parseFrom(message.getBody());
        
        session.updateActiveTime();

        return S2C_Heartbeat.newBuilder()
                .setResult(buildSuccessResult())
                .setServerTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 处理踢下线
     */
    public void kickOut(Session session, int reason, String message) {
        log.info("踢下线: sessionId={}, roleId={}, reason={}", 
                session.getSessionId(), session.getRoleId(), reason);

        S2C_KickOut push = S2C_KickOut.newBuilder()
                .setReason(reason)
                .setMessage(message)
                .build();

        // TODO: 发送踢下线推送
        // session.sendPush(0xF001, push.toByteArray());

        // 关闭连接
        session.close();
    }

    private com.game.proto.Result buildSuccessResult() {
        return com.game.proto.Result.newBuilder()
                .setCode(0)
                .setMessage("success")
                .build();
    }
}
