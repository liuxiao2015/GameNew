package com.game.core.push;

import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * 消息推送服务
 * <p>
 * 提供便捷的消息推送能力：
 * <ul>
 *     <li>推送给单个玩家</li>
 *     <li>推送给多个玩家</li>
 *     <li>广播给所有在线玩家</li>
 *     <li>条件过滤推送</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 推送给单个玩家
 * pushService.push(roleId, 0x0201, playerInfoProto);
 *
 * // 推送给多个玩家
 * pushService.push(Arrays.asList(roleId1, roleId2, roleId3), 0x0301, chatMessageProto);
 *
 * // 广播给所有玩家
 * pushService.broadcast(0x0101, serverNoticeProto);
 *
 * // 条件推送 (例如：等级大于 50 的玩家)
 * pushService.pushIf(session -> session.getLevel() > 50, 0x0401, activityProto);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final SessionManager sessionManager;

    // ==================== 单人推送 ====================

    /**
     * 推送 Protobuf 消息给玩家
     *
     * @param roleId     角色 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     */
    public void push(long roleId, int protocolId, Message message) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session != null && session.isActive()) {
            pushToSession(session, protocolId, message);
        }
    }

    /**
     * 推送消息给玩家 (带方法号)
     */
    public void push(long roleId, int moduleId, int methodId, Message message) {
        push(roleId, (moduleId << 8) | methodId, message);
    }

    /**
     * 推送字节数组消息
     */
    public void push(long roleId, int protocolId, byte[] data) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session != null && session.isActive()) {
            session.sendPush(0, protocolId, data);
        }
    }

    /**
     * 推送给 Session
     */
    public void pushToSession(Session session, int protocolId, Message message) {
        if (session == null || !session.isActive()) {
            return;
        }

        GameMessage gameMessage = GameMessage.createPush(0, protocolId, message.toByteArray());
        session.send(gameMessage);
    }

    // ==================== 多人推送 ====================

    /**
     * 推送给多个玩家
     */
    public void push(Collection<Long> roleIds, int protocolId, Message message) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        byte[] data = message.toByteArray();
        for (Long roleId : roleIds) {
            Session session = sessionManager.getSessionByRoleId(roleId);
            if (session != null && session.isActive()) {
                session.sendPush(0, protocolId, data);
            }
        }
    }

    /**
     * 推送给多个玩家 (带方法号)
     */
    public void push(Collection<Long> roleIds, int moduleId, int methodId, Message message) {
        push(roleIds, (moduleId << 8) | methodId, message);
    }

    // ==================== 广播 ====================

    /**
     * 广播给所有在线玩家
     */
    public void broadcast(int protocolId, Message message) {
        byte[] data = message.toByteArray();
        for (Session session : sessionManager.getAllRoleSessions()) {
            if (session.isActive()) {
                session.sendPush(0, protocolId, data);
            }
        }
    }

    /**
     * 广播给所有在线玩家 (带方法号)
     */
    public void broadcast(int moduleId, int methodId, Message message) {
        broadcast((moduleId << 8) | methodId, message);
    }

    /**
     * 广播字节数组
     */
    public void broadcast(int protocolId, byte[] data) {
        for (Session session : sessionManager.getAllRoleSessions()) {
            if (session.isActive()) {
                session.sendPush(0, protocolId, data);
            }
        }
    }

    // ==================== 条件推送 ====================

    /**
     * 条件推送
     *
     * @param filter     Session 过滤条件
     * @param protocolId 协议号
     * @param message    消息
     */
    public void pushIf(Predicate<Session> filter, int protocolId, Message message) {
        byte[] data = message.toByteArray();
        for (Session session : sessionManager.getAllRoleSessions()) {
            if (session.isActive() && filter.test(session)) {
                session.sendPush(0, protocolId, data);
            }
        }
    }

    /**
     * 推送给指定服务器的玩家
     */
    public void pushByServerId(int serverId, int protocolId, Message message) {
        pushIf(session -> session.getServerId() == serverId, protocolId, message);
    }

    // ==================== 统计 ====================

    /**
     * 获取在线人数
     */
    public int getOnlineCount() {
        return sessionManager.getOnlineCount();
    }

    /**
     * 检查玩家是否在线
     */
    public boolean isOnline(long roleId) {
        return sessionManager.isOnline(roleId);
    }
}
