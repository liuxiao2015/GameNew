package com.game.gateway.handler;

import com.game.common.protocol.ProtocolConstants;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import com.game.gateway.route.MessageRouter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网关通道处理器
 *
 * @author GameServer
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class GatewayChannelHandler extends SimpleChannelInboundHandler<GameMessage> {

    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Session session = sessionManager.createSession(ctx.channel());
        log.info("客户端连接: sessionId={}, remote={}", session.getSessionId(), ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Session session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            log.info("客户端断开: sessionId={}, roleId={}", session.getSessionId(), session.getRoleId());
            
            // 通知业务服务玩家下线
            if (session.getRoleId() > 0) {
                messageRouter.notifyPlayerOffline(session);
            }
            
            sessionManager.removeSession(ctx.channel());
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) throws Exception {
        Session session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            log.warn("未找到会话, 关闭连接: remote={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        // 更新活跃时间
        session.updateActiveTime();

        // 心跳处理
        if (msg.getCmd() == ProtocolConstants.CMD_HEARTBEAT) {
            handleHeartbeat(ctx, session, msg);
            return;
        }

        // 路由消息到业务服务
        messageRouter.route(session, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("读超时, 关闭连接: remote={}", ctx.channel().remoteAddress());
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 发送心跳
                GameMessage heartbeat = new GameMessage();
                heartbeat.setCmd(ProtocolConstants.CMD_HEARTBEAT);
                heartbeat.setSeq(0);
                heartbeat.setData(new byte[0]);
                ctx.writeAndFlush(heartbeat);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Session session = sessionManager.getSession(ctx.channel());
        log.error("连接异常: sessionId={}, remote={}, error={}",
            session != null ? session.getSessionId() : "null",
            ctx.channel().remoteAddress(),
            cause.getMessage());
        ctx.close();
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, Session session, GameMessage msg) {
        GameMessage response = new GameMessage();
        response.setCmd(ProtocolConstants.CMD_HEARTBEAT);
        response.setSeq(msg.getSeq());
        response.setData(new byte[0]);
        ctx.writeAndFlush(response);
    }
}
