package com.game.gateway.push;

import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 消息推送服务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushService {

    private final SessionManager sessionManager;

    /**
     * 推送消息给指定玩家
     */
    public boolean pushToPlayer(long roleId, GameMessage message) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session == null || !session.isActive()) {
            log.debug("玩家不在线: roleId={}", roleId);
            return false;
        }

        session.send(message);
        return true;
    }

    /**
     * 推送消息给多个玩家
     */
    public void pushToPlayers(List<Long> roleIds, GameMessage message) {
        for (long roleId : roleIds) {
            pushToPlayer(roleId, message);
        }
    }

    /**
     * 广播消息给所有在线玩家
     */
    public void broadcast(GameMessage message) {
        Collection<Session> sessions = sessionManager.getAllSessions();
        for (Session session : sessions) {
            if (session.isActive() && session.getRoleId() > 0) {
                session.send(message);
            }
        }
        log.info("广播消息: cmd={}, onlineCount={}", message.getCmd(), sessions.size());
    }

    /**
     * 广播消息给指定服务器的玩家
     */
    public void broadcastToServer(int serverId, GameMessage message) {
        Collection<Session> sessions = sessionManager.getAllSessions();
        int count = 0;
        for (Session session : sessions) {
            if (session.isActive() && session.getRoleId() > 0 && session.getServerId() == serverId) {
                session.send(message);
                count++;
            }
        }
        log.info("服务器广播: cmd={}, serverId={}, count={}", message.getCmd(), serverId, count);
    }
}
