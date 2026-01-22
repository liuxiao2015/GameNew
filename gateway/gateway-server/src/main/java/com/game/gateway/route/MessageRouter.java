package com.game.gateway.route;

import com.game.common.protocol.ProtocolConstants;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息路由器
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    /**
     * 路由线程池
     */
    private final ExecutorService routeExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 路由消息
     */
    public void route(Session session, GameMessage message) {
        CompletableFuture.runAsync(() -> {
            try {
                doRoute(session, message);
            } catch (Exception e) {
                log.error("消息路由异常: cmd={}, sessionId={}", message.getCmd(), session.getSessionId(), e);
                sendError(session, message, e);
            }
        }, routeExecutor);
    }

    /**
     * 执行路由
     */
    private void doRoute(Session session, GameMessage message) {
        int cmd = message.getCmd();

        // 根据命令号确定目标服务
        int module = cmd / 1000;

        switch (module) {
            case ProtocolConstants.MODULE_LOGIN -> routeToLogin(session, message);
            case ProtocolConstants.MODULE_PLAYER -> routeToGame(session, message);
            case ProtocolConstants.MODULE_GUILD -> routeToGuild(session, message);
            case ProtocolConstants.MODULE_CHAT -> routeToChat(session, message);
            case ProtocolConstants.MODULE_RANK -> routeToRank(session, message);
            default -> {
                log.warn("未知的协议模块: cmd={}, module={}", cmd, module);
                sendError(session, message, new RuntimeException("未知的协议模块"));
            }
        }
    }

    /**
     * 路由到登录服务
     */
    private void routeToLogin(Session session, GameMessage message) {
        // 通过 Dubbo 调用登录服务
        log.debug("路由到登录服务: cmd={}, sessionId={}", message.getCmd(), session.getSessionId());
        // 实际实现: 调用 LoginService
    }

    /**
     * 路由到游戏服务
     */
    private void routeToGame(Session session, GameMessage message) {
        // 验证登录状态
        if (session.getRoleId() <= 0) {
            log.warn("未登录, 无法访问游戏服务: sessionId={}", session.getSessionId());
            sendError(session, message, new RuntimeException("请先登录"));
            return;
        }

        // 通过一致性 Hash 路由到指定实例
        log.debug("路由到游戏服务: cmd={}, roleId={}", message.getCmd(), session.getRoleId());
        // 实际实现: 使用一致性 Hash 路由到玩家所在的 Actor
    }

    /**
     * 路由到公会服务
     */
    private void routeToGuild(Session session, GameMessage message) {
        if (session.getRoleId() <= 0) {
            sendError(session, message, new RuntimeException("请先登录"));
            return;
        }

        log.debug("路由到公会服务: cmd={}, roleId={}", message.getCmd(), session.getRoleId());
        // 实际实现: 调用 GuildService
    }

    /**
     * 路由到聊天服务
     */
    private void routeToChat(Session session, GameMessage message) {
        if (session.getRoleId() <= 0) {
            sendError(session, message, new RuntimeException("请先登录"));
            return;
        }

        log.debug("路由到聊天服务: cmd={}, roleId={}", message.getCmd(), session.getRoleId());
        // 实际实现: 调用 ChatService
    }

    /**
     * 路由到排行服务
     */
    private void routeToRank(Session session, GameMessage message) {
        log.debug("路由到排行服务: cmd={}", message.getCmd());
        // 实际实现: 调用 RankService
    }

    /**
     * 通知玩家下线
     */
    public void notifyPlayerOffline(Session session) {
        if (session.getRoleId() > 0) {
            log.info("通知玩家下线: roleId={}", session.getRoleId());
            // 实际实现: 调用 PlayerService.offline()
        }
    }

    /**
     * 发送错误响应
     */
    private void sendError(Session session, GameMessage request, Exception e) {
        // 实际实现: 构建错误响应并发送
        log.error("处理请求失败: cmd={}, error={}", request.getCmd(), e.getMessage());
    }
}
