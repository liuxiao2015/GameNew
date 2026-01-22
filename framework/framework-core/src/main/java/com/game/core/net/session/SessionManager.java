package com.game.core.net.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 * <p>
 * 支持断线重连机制：
 * <ul>
 *     <li>维护 reconnectToken -> Session 映射</li>
 *     <li>定期清理超时的断线 Session</li>
 *     <li>重连时恢复原有会话状态</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class SessionManager {

    /**
     * Channel 关联的 Session 属性 Key
     */
    public static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session");

    /**
     * Session ID -> Session
     */
    private final Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 角色 ID -> Session
     */
    private final Map<Long, Session> roleSessionMap = new ConcurrentHashMap<>();

    /**
     * 重连 Token -> Session (用于断线重连)
     */
    private final Map<String, Session> reconnectTokenMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("SessionManager 初始化完成");
    }

    /**
     * 创建 Session
     */
    public Session createSession(Channel channel) {
        Session session = new Session(channel);
        channel.attr(SESSION_KEY).set(session);
        sessionMap.put(session.getSessionId(), session);
        reconnectTokenMap.put(session.getReconnectToken(), session);

        log.info("创建 Session: sessionId={}, remoteAddress={}, token={}",
                session.getSessionId(), session.getRemoteAddress(), session.getReconnectToken());
        return session;
    }

    /**
     * 从 Channel 获取 Session
     */
    public Session getSession(Channel channel) {
        return channel.attr(SESSION_KEY).get();
    }

    /**
     * 根据 Session ID 获取 Session
     */
    public Session getSession(long sessionId) {
        return sessionMap.get(sessionId);
    }

    /**
     * 根据角色 ID 获取 Session
     */
    public Session getSessionByRoleId(long roleId) {
        return roleSessionMap.get(roleId);
    }

    /**
     * 根据重连 Token 获取 Session
     */
    public Session getSessionByReconnectToken(String token) {
        return reconnectTokenMap.get(token);
    }

    /**
     * 断线重连
     *
     * @param token      重连 Token
     * @param newChannel 新的 Channel
     * @return 重连后的 Session，失败返回 null
     */
    public Session reconnect(String token, Channel newChannel) {
        Session session = reconnectTokenMap.get(token);
        if (session == null) {
            log.warn("重连失败，Token 不存在: token={}", token);
            return null;
        }

        if (!session.isDisconnected()) {
            log.warn("重连失败，Session 未断线: sessionId={}, token={}", session.getSessionId(), token);
            return null;
        }

        if (session.isReconnectTimeout()) {
            log.warn("重连失败，已超时: sessionId={}, token={}", session.getSessionId(), token);
            removeSession(session);
            return null;
        }

        // 重连成功
        session.reconnect(newChannel);
        newChannel.attr(SESSION_KEY).set(session);

        log.info("重连成功: sessionId={}, roleId={}, newAddress={}",
                session.getSessionId(), session.getRoleId(), session.getRemoteAddress());

        return session;
    }

    /**
     * 处理连接断开 (可能是临时断线)
     */
    public void onChannelInactive(Channel channel) {
        Session session = getSession(channel);
        if (session == null) {
            return;
        }

        // 如果已登录角色，标记为断线状态而非直接移除
        if (session.hasRole()) {
            session.markDisconnected();
            log.info("Session 断线，等待重连: sessionId={}, roleId={}, timeout={}ms",
                    session.getSessionId(), session.getRoleId(), Session.RECONNECT_TIMEOUT_MS);
        } else {
            // 未登录的 Session 直接移除
            removeSession(session);
        }
    }

    /**
     * 绑定角色到 Session
     */
    public void bindRole(Session session, long roleId, String roleName) {
        // 检查是否有旧 Session
        Session oldSession = roleSessionMap.get(roleId);
        if (oldSession != null && oldSession != session) {
            log.info("踢出旧连接: roleId={}, oldSessionId={}, newSessionId={}",
                    roleId, oldSession.getSessionId(), session.getSessionId());
            // 通知旧连接被踢出
            kickSession(oldSession, "异地登录");
        }

        session.setRoleId(roleId);
        session.setRoleName(roleName);
        session.setAuthenticated(true);
        roleSessionMap.put(roleId, session);

        log.info("绑定角色: sessionId={}, roleId={}, roleName={}",
                session.getSessionId(), roleId, roleName);
    }

    /**
     * 解绑角色
     */
    public void unbindRole(Session session) {
        if (session.getRoleId() > 0) {
            roleSessionMap.remove(session.getRoleId());
            log.info("解绑角色: sessionId={}, roleId={}",
                    session.getSessionId(), session.getRoleId());
            session.setRoleId(0);
            session.setRoleName(null);
            session.setAuthenticated(false);
        }
    }

    /**
     * 移除 Session
     */
    public void removeSession(Session session) {
        if (session == null) {
            return;
        }

        sessionMap.remove(session.getSessionId());
        reconnectTokenMap.remove(session.getReconnectToken());
        if (session.getRoleId() > 0) {
            roleSessionMap.remove(session.getRoleId());
        }

        log.info("移除 Session: sessionId={}, roleId={}",
                session.getSessionId(), session.getRoleId());
    }

    /**
     * 移除 Session (通过 Channel)
     */
    public void removeSession(Channel channel) {
        Session session = getSession(channel);
        if (session != null) {
            removeSession(session);
        }
    }

    /**
     * 踢出 Session
     */
    public void kickSession(Session session, String reason) {
        if (session == null) {
            return;
        }

        log.info("踢出 Session: sessionId={}, roleId={}, reason={}",
                session.getSessionId(), session.getRoleId(), reason);

        // TODO: 发送踢出通知消息
        // session.sendPush(PushType.SERVER_KICK, ...);

        removeSession(session);
        session.close();
    }

    /**
     * 踢出角色
     */
    public void kickRole(long roleId, String reason) {
        Session session = roleSessionMap.get(roleId);
        kickSession(session, reason);
    }

    /**
     * 判断角色是否在线 (包括断线重连中)
     */
    public boolean isOnline(long roleId) {
        Session session = roleSessionMap.get(roleId);
        if (session == null) {
            return false;
        }
        // 断线但未超时也算在线
        return session.isActive() || (session.isDisconnected() && !session.isReconnectTimeout());
    }

    /**
     * 判断角色是否真正在线 (有活跃连接)
     */
    public boolean isReallyOnline(long roleId) {
        Session session = roleSessionMap.get(roleId);
        return session != null && session.isActive();
    }

    /**
     * 获取在线人数 (包括断线重连中)
     */
    public int getOnlineCount() {
        return roleSessionMap.size();
    }

    /**
     * 获取真正在线人数 (有活跃连接)
     */
    public int getReallyOnlineCount() {
        return (int) roleSessionMap.values().stream()
                .filter(Session::isActive)
                .count();
    }

    /**
     * 获取断线重连中人数
     */
    public int getReconnectingCount() {
        return (int) roleSessionMap.values().stream()
                .filter(s -> s.isDisconnected() && !s.isReconnectTimeout())
                .count();
    }

    /**
     * 获取所有在线 Session
     */
    public Collection<Session> getAllSessions() {
        return sessionMap.values();
    }

    /**
     * 获取所有在线角色 Session
     */
    public Collection<Session> getAllRoleSessions() {
        return roleSessionMap.values();
    }

    /**
     * 广播消息到所有在线玩家
     */
    public void broadcast(int pushType, int protocolId, byte[] body) {
        for (Session session : roleSessionMap.values()) {
            if (session.isActive()) {
                session.sendPush(pushType, protocolId, body);
            }
        }
    }

    /**
     * 定期清理超时的断线 Session (每分钟执行一次)
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupTimeoutSessions() {
        int cleanedCount = 0;

        Iterator<Map.Entry<Long, Session>> iterator = roleSessionMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next().getValue();
            if (session.isDisconnected() && session.isReconnectTimeout()) {
                iterator.remove();
                sessionMap.remove(session.getSessionId());
                reconnectTokenMap.remove(session.getReconnectToken());
                cleanedCount++;

                log.info("清理超时断线 Session: sessionId={}, roleId={}",
                        session.getSessionId(), session.getRoleId());
            }
        }

        if (cleanedCount > 0) {
            log.info("清理超时断线 Session 完成: count={}", cleanedCount);
        }
    }

    /**
     * 获取会话统计信息
     */
    public String getStats() {
        return String.format("total=%d, online=%d, reconnecting=%d",
                sessionMap.size(), getReallyOnlineCount(), getReconnectingCount());
    }
}
