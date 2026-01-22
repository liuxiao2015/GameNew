package com.game.core.net.handler;

import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏消息处理器
 *
 * @author GameServer
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class GameChannelHandler extends SimpleChannelInboundHandler<GameMessage> {

    private final SessionManager sessionManager;
    private final MessageDispatcher messageDispatcher;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Session session = sessionManager.createSession(ctx.channel());
        log.info("客户端连接: {}", session.getRemoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Session session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            log.info("客户端断开: sessionId={}, roleId={}, address={}", 
                    session.getSessionId(), session.getRoleId(), session.getRemoteAddress());

            // 触发断线事件
            messageDispatcher.onSessionClose(session);

            // 移除 Session
            sessionManager.removeSession(session);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) {
        Session session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            log.warn("Session 不存在: channel={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        // 更新活跃时间
        session.updateActiveTime();

        // 分发消息
        messageDispatcher.dispatch(session, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Session session = sessionManager.getSession(ctx.channel());
                if (session != null) {
                    log.warn("Session 读空闲超时，关闭连接: sessionId={}, roleId={}", 
                            session.getSessionId(), session.getRoleId());
                }
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Session session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            log.error("Channel 异常: sessionId={}, roleId={}", 
                    session.getSessionId(), session.getRoleId(), cause);
        } else {
            log.error("Channel 异常: channel={}", ctx.channel().remoteAddress(), cause);
        }
        ctx.close();
    }
}
