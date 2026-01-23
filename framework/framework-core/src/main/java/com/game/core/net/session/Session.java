package com.game.core.net.session;

import com.game.core.net.codec.GameMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端会话
 * <p>
 * 支持断线重连机制：
 * <ul>
 *     <li>每个 Session 生成唯一的 reconnectToken</li>
 *     <li>客户端断线后可使用 token 恢复会话</li>
 *     <li>重连时保留原有状态 (角色信息、属性等)</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Getter
public class Session {

    /**
     * Session ID 生成器
     */
    private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(0);

    /**
     * 重连超时时间 (毫秒) - 默认 5 分钟
     */
    public static final long RECONNECT_TIMEOUT_MS = 5 * 60 * 1000;

    // ==================== 基础信息 ====================

    /**
     * Session ID
     */
    private final long sessionId;

    /**
     * Netty Channel (可能在重连后更换)
     */
    @Setter
    private volatile Channel channel;

    /**
     * 重连 Token (用于断线重连)
     */
    private final String reconnectToken;

    /**
     * 创建时间
     */
    private final long createTime;

    /**
     * 最后活跃时间
     */
    @Setter
    private volatile long lastActiveTime;

    /**
     * 断线时间 (0 表示未断线)
     */
    @Setter
    private volatile long disconnectTime;

    // ==================== 账号信息 ====================

    /**
     * 账号 ID
     */
    @Setter
    private long accountId;

    /**
     * 角色 ID
     */
    @Setter
    private long roleId;

    /**
     * 角色名
     */
    @Setter
    private String roleName;

    /**
     * 服务器 ID
     */
    @Setter
    private int serverId;

    /**
     * 是否已认证
     */
    @Setter
    private boolean authenticated;

    /**
     * 登录 Token (用于验证)
     */
    @Setter
    private String token;

    // ==================== 扩展属性 ====================

    /**
     * 扩展属性
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 待发送消息队列 (断线期间缓存)
     */
    private final Map<Integer, GameMessage> pendingMessages = new ConcurrentHashMap<>();

    /**
     * 消息序号计数器
     */
    private final AtomicLong messageSeqCounter = new AtomicLong(0);

    public Session(Channel channel) {
        this.sessionId = SESSION_ID_GENERATOR.incrementAndGet();
        this.channel = channel;
        this.reconnectToken = generateReconnectToken();
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
        this.disconnectTime = 0;
    }

    /**
     * 生成重连 Token
     */
    private String generateReconnectToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取远程地址
     */
    public String getRemoteAddress() {
        if (channel != null && channel.remoteAddress() != null) {
            return channel.remoteAddress().toString();
        }
        return "unknown";
    }

    /**
     * 判断连接是否活跃
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    /**
     * 判断是否已登录 (已认证且有账号ID)
     */
    public boolean isLoggedIn() {
        return authenticated && accountId > 0;
    }

    /**
     * 判断是否已选择角色
     */
    public boolean hasRole() {
        return roleId > 0;
    }

    /**
     * 判断是否断线中 (可重连)
     */
    public boolean isDisconnected() {
        return disconnectTime > 0;
    }

    /**
     * 判断重连是否超时
     */
    public boolean isReconnectTimeout() {
        if (disconnectTime <= 0) {
            return false;
        }
        return System.currentTimeMillis() - disconnectTime > RECONNECT_TIMEOUT_MS;
    }

    /**
     * 标记断线
     */
    public void markDisconnected() {
        this.disconnectTime = System.currentTimeMillis();
        this.channel = null;
        log.info("Session 断线: sessionId={}, roleId={}, token={}",
                sessionId, roleId, reconnectToken);
    }

    /**
     * 重连成功
     */
    public void reconnect(Channel newChannel) {
        this.channel = newChannel;
        this.disconnectTime = 0;
        this.lastActiveTime = System.currentTimeMillis();
        log.info("Session 重连成功: sessionId={}, roleId={}", sessionId, roleId);

        // 发送缓存的消息
        flushPendingMessages();
    }

    /**
     * 发送消息
     */
    public void send(GameMessage message) {
        if (!isActive()) {
            if (isDisconnected() && !isReconnectTimeout()) {
                // 断线但未超时，缓存消息
                int seq = (int) messageSeqCounter.incrementAndGet();
                pendingMessages.put(seq, message);
                log.debug("Session 断线中，消息已缓存: sessionId={}, seq={}", sessionId, seq);
                return;
            }
            log.warn("Session 已关闭，无法发送消息: sessionId={}, roleId={}", sessionId, roleId);
            return;
        }
        channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("发送消息失败: sessionId={}, roleId={}", sessionId, roleId, future.cause());
            }
        });
    }

    /**
     * 发送响应
     */
    public void sendResponse(int seqId, int errorCode, byte[] body) {
        send(GameMessage.createResponse(seqId, errorCode, body));
    }

    /**
     * 发送推送
     */
    public void sendPush(int pushType, int protocolId, byte[] body) {
        send(GameMessage.createPush(pushType, protocolId, body));
    }

    /**
     * 发送缓存的消息
     */
    private void flushPendingMessages() {
        if (pendingMessages.isEmpty()) {
            return;
        }
        log.info("发送缓存消息: sessionId={}, count={}", sessionId, pendingMessages.size());
        pendingMessages.values().forEach(this::send);
        pendingMessages.clear();
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        pendingMessages.clear();
    }

    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 移除属性
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * 判断是否有指定属性
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", remoteAddress=" + getRemoteAddress() +
                ", authenticated=" + authenticated +
                ", disconnected=" + isDisconnected() +
                '}';
    }
}
