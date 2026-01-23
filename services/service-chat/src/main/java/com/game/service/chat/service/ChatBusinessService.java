package com.game.service.chat.service;

import com.game.api.common.ProtocolConstants;
import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.core.cache.CacheService;
import com.game.core.id.IdService;
import com.game.core.push.PushService;
import com.game.core.ratelimit.RateLimiterService;
import com.game.core.security.SecurityFilter;
import com.game.data.redis.RedisService;
import com.game.proto.ChatMessage;
import com.game.proto.S2C_ChatPush;
import com.game.service.chat.entity.MuteInfo;
import com.game.service.chat.repository.ChatMessageRepository;
import com.game.service.chat.repository.MuteInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天业务服务
 * <p>
 * 提供聊天相关的业务逻辑，演示框架各项能力的使用：
 * <ul>
 *     <li>PushService: 广播聊天消息给玩家</li>
 *     <li>RedisService: 消息缓存和频道发布</li>
 *     <li>RateLimiterService: 聊天频率限制</li>
 *     <li>SecurityFilter: 敏感词过滤</li>
 *     <li>CacheService: 玩家信息缓存</li>
 *     <li>RPC: 跨服务获取玩家信息</li>
 *     <li>IdService: 生成消息唯一 ID</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBusinessService {

    private final PushService pushService;
    private final RedisService redisService;
    private final RateLimiterService rateLimiterService;
    private final SecurityFilter securityFilter;
    private final CacheService cacheService;
    private final IdService idService;
    private final ChatMessageRepository chatMessageRepository;
    private final MuteInfoRepository muteInfoRepository;

    @DubboReference(check = false)
    private PlayerService playerService;

    /**
     * 聊天频道
     */
    public static final int CHANNEL_WORLD = 1;    // 世界频道
    public static final int CHANNEL_GUILD = 2;    // 公会频道
    public static final int CHANNEL_PRIVATE = 3;  // 私聊频道
    public static final int CHANNEL_SYSTEM = 4;   // 系统频道

    /**
     * 推送协议号
     */
    private static final int PUSH_CHAT = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x10;

    /**
     * 聊天消息缓存 Key
     */
    private static final String CHAT_HISTORY_KEY = "chat:history:";

    // ==================== 发送消息 ====================

    /**
     * 发送聊天消息
     */
    public SendChatResult sendChat(long roleId, int channel, String content, long targetId) {
        // 1. 频率限制
        String rateLimitKey = "chat:" + roleId;
        if (!rateLimiterService.tryAcquire(rateLimitKey, 10, Duration.ofSeconds(60))) {
            return SendChatResult.fail("发送太频繁，请稍后再试");
        }

        // 2. 检查禁言
        if (isMuted(roleId)) {
            return SendChatResult.fail("您已被禁言");
        }

        // 3. 获取发送者信息 (使用缓存)
        PlayerDTO sender = getPlayerWithCache(roleId);
        if (sender == null) {
            return SendChatResult.fail("获取玩家信息失败");
        }

        // 4. 敏感词过滤
        String filteredContent = securityFilter.filterSensitiveWords(content);

        // 5. 生成消息 ID
        long msgId = idService.nextId();

        // 6. 构建聊天消息
        ChatMessage message = ChatMessage.newBuilder()
                .setMsgId(msgId)
                .setChannel(channel)
                .setSenderId(roleId)
                .setSenderName(sender.getRoleName())
                .setSenderLevel(sender.getLevel())
                .setSenderAvatar(sender.getAvatarId())
                .setSenderVip(sender.getVipLevel())
                .setContent(filteredContent)
                .setSendTime(System.currentTimeMillis())
                .setTargetId(targetId)
                .build();

        // 7. 根据频道类型处理
        switch (channel) {
            case CHANNEL_WORLD -> broadcastWorld(message);
            case CHANNEL_GUILD -> broadcastGuild(sender.getGuildId(), message);
            case CHANNEL_PRIVATE -> sendPrivate(roleId, targetId, message);
            default -> {
                return SendChatResult.fail("无效的频道类型");
            }
        }

        // 8. 保存消息记录 (异步)
        saveMessageAsync(channel, targetId, message);

        log.debug("发送聊天消息成功: msgId={}, channel={}, roleId={}", msgId, channel, roleId);

        return SendChatResult.success(msgId);
    }

    /**
     * 世界频道广播
     */
    private void broadcastWorld(ChatMessage message) {
        S2C_ChatPush push = S2C_ChatPush.newBuilder()
                .setMessage(message)
                .build();
        
        // 广播给所有在线玩家
        pushService.broadcast(PUSH_CHAT, push);

        // 同时通过 Redis 发布，支持跨服务器
        redisService.publish("chat:world", message.toByteArray());

        // 缓存最近消息
        String cacheKey = CHAT_HISTORY_KEY + "world";
        redisService.leftPush(cacheKey, message.toByteArray());
        redisService.trim(cacheKey, 0, 99); // 只保留最近 100 条
        redisService.expire(cacheKey, Duration.ofHours(24));
    }

    /**
     * 公会频道广播
     */
    private void broadcastGuild(long guildId, ChatMessage message) {
        if (guildId <= 0) {
            return;
        }

        S2C_ChatPush push = S2C_ChatPush.newBuilder()
                .setMessage(message)
                .build();

        // 获取公会成员列表并推送
        // TODO: 从公会服务获取成员列表
        // 这里简化处理，通过 Redis 发布
        redisService.publish("chat:guild:" + guildId, message.toByteArray());

        // 缓存公会消息
        String cacheKey = CHAT_HISTORY_KEY + "guild:" + guildId;
        redisService.leftPush(cacheKey, message.toByteArray());
        redisService.trim(cacheKey, 0, 99);
        redisService.expire(cacheKey, Duration.ofHours(24));
    }

    /**
     * 私聊消息
     */
    private void sendPrivate(long senderId, long targetId, ChatMessage message) {
        S2C_ChatPush push = S2C_ChatPush.newBuilder()
                .setMessage(message)
                .build();

        // 推送给发送者和接收者
        pushService.push(senderId, PUSH_CHAT, push);
        pushService.push(targetId, PUSH_CHAT, push);

        // 缓存私聊消息 (使用较小的 ID 在前保证唯一性)
        long minId = Math.min(senderId, targetId);
        long maxId = Math.max(senderId, targetId);
        String cacheKey = CHAT_HISTORY_KEY + "private:" + minId + ":" + maxId;
        redisService.leftPush(cacheKey, message.toByteArray());
        redisService.trim(cacheKey, 0, 49); // 私聊保留 50 条
        redisService.expire(cacheKey, Duration.ofDays(7));
    }

    /**
     * 发送系统消息
     */
    public void sendSystemMessage(String content, int noticeType) {
        ChatMessage message = ChatMessage.newBuilder()
                .setMsgId(idService.nextId())
                .setChannel(CHANNEL_SYSTEM)
                .setSenderId(0)
                .setSenderName("系统")
                .setContent(content)
                .setSendTime(System.currentTimeMillis())
                .build();

        S2C_ChatPush push = S2C_ChatPush.newBuilder()
                .setMessage(message)
                .build();

        pushService.broadcast(PUSH_CHAT, push);
    }

    // ==================== 获取历史消息 ====================

    /**
     * 获取聊天历史
     */
    public List<ChatMessage> getChatHistory(long roleId, int channel, long targetId, 
                                            long lastMsgId, int count) {
        String cacheKey;

        switch (channel) {
            case CHANNEL_WORLD -> cacheKey = CHAT_HISTORY_KEY + "world";
            case CHANNEL_GUILD -> {
                PlayerDTO player = getPlayerWithCache(roleId);
                if (player == null || player.getGuildId() <= 0) {
                    return Collections.emptyList();
                }
                cacheKey = CHAT_HISTORY_KEY + "guild:" + player.getGuildId();
            }
            case CHANNEL_PRIVATE -> {
                long minId = Math.min(roleId, targetId);
                long maxId = Math.max(roleId, targetId);
                cacheKey = CHAT_HISTORY_KEY + "private:" + minId + ":" + maxId;
            }
            default -> {
                return Collections.emptyList();
            }
        }

        // 从 Redis 获取缓存的消息
        List<byte[]> cachedMessages = redisService.range(cacheKey, 0, count - 1);
        if (cachedMessages == null || cachedMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (byte[] data : cachedMessages) {
            try {
                ChatMessage msg = ChatMessage.parseFrom(data);
                // 过滤掉 lastMsgId 之前的消息
                if (lastMsgId <= 0 || msg.getMsgId() < lastMsgId) {
                    messages.add(msg);
                }
            } catch (Exception e) {
                log.warn("解析聊天消息失败", e);
            }
        }

        return messages;
    }

    // ==================== 禁言管理 ====================

    /**
     * 检查是否被禁言
     */
    public boolean isMuted(long roleId) {
        MuteInfo muteInfo = muteInfoRepository.findByRoleId(roleId);
        if (muteInfo == null) {
            return false;
        }
        if (muteInfo.getMuteEndTime() < 0) {
            return true; // 永久禁言
        }
        return System.currentTimeMillis() < muteInfo.getMuteEndTime();
    }

    /**
     * 禁言玩家
     */
    public void mutePlayer(long roleId, long duration, String reason) {
        MuteInfo muteInfo = muteInfoRepository.findByRoleId(roleId);
        if (muteInfo == null) {
            muteInfo = new MuteInfo();
            muteInfo.setRoleId(roleId);
        }

        muteInfo.setMuteEndTime(duration <= 0 ? -1 : System.currentTimeMillis() + duration * 1000);
        muteInfo.setReason(reason);
        muteInfo.setMuteTime(System.currentTimeMillis());
        muteInfoRepository.save(muteInfo);

        log.info("禁言玩家: roleId={}, duration={}, reason={}", roleId, duration, reason);
    }

    /**
     * 解除禁言
     */
    public void unmutePlayer(long roleId) {
        muteInfoRepository.deleteByRoleId(roleId);
        log.info("解除禁言: roleId={}", roleId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取玩家信息 (带缓存)
     */
    private PlayerDTO getPlayerWithCache(long roleId) {
        String cacheKey = "player:" + roleId;
        return cacheService.get(cacheKey, () -> {
            Result<PlayerDTO> result = playerService.getPlayerInfo(roleId);
            return result.isSuccess() ? result.getData() : null;
        }, Duration.ofMinutes(5));
    }

    /**
     * 异步保存消息
     */
    private void saveMessageAsync(int channel, long targetId, ChatMessage message) {
        // 使用虚拟线程异步保存
        Thread.startVirtualThread(() -> {
            try {
                com.game.service.chat.entity.ChatMessage entity = 
                        new com.game.service.chat.entity.ChatMessage();
                entity.setMsgId(message.getMsgId());
                entity.setChannel(channel);
                entity.setSenderId(message.getSenderId());
                entity.setSenderName(message.getSenderName());
                entity.setContent(message.getContent());
                entity.setSendTime(message.getSendTime());
                entity.setTargetId(targetId);
                chatMessageRepository.save(entity);
            } catch (Exception e) {
                log.error("保存聊天消息失败: msgId={}", message.getMsgId(), e);
            }
        });
    }

    // ==================== 数据类 ====================

    public record SendChatResult(boolean success, long msgId, String message) {
        public static SendChatResult success(long msgId) {
            return new SendChatResult(true, msgId, null);
        }

        public static SendChatResult fail(String message) {
            return new SendChatResult(false, 0, message);
        }
    }
}
