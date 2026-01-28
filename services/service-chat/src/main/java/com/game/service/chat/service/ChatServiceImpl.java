package com.game.service.chat.service;

import com.game.api.chat.ChatMessageDTO;
import com.game.api.chat.ChatService;
import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.IdGenerator;
import com.game.data.redis.RedisService;
import com.game.mq.MqExchange;
import com.game.mq.message.ChatMqMessage;
import com.game.mq.producer.MqProducer;
import com.game.service.chat.entity.ChatMessage;
import com.game.service.chat.entity.MuteInfo;
import com.game.service.chat.repository.ChatMessageRepository;
import com.game.service.chat.repository.MuteInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /**
     * 频道常量
     */
    private static final int CHANNEL_WORLD = 1;
    private static final int CHANNEL_GUILD = 2;
    private static final int CHANNEL_PRIVATE = 3;
    private static final int CHANNEL_SYSTEM = 4;

    /**
     * 缓存 Key
     */
    private static final String MUTE_KEY = "chat:mute:";
    private static final String WORLD_CHAT_LIMIT_KEY = "chat:limit:world:";

    /**
     * 世界频道发言间隔 (秒)
     */
    private static final int WORLD_CHAT_INTERVAL = 5;

    private final ChatMessageRepository chatMessageRepository;
    private final MuteInfoRepository muteInfoRepository;
    private final RedisService redisService;
    private final IdGenerator idGenerator;
    private final MqProducer mqProducer;

    @DubboReference(check = false)
    private PlayerService playerService;

    @Override
    public Result<Long> sendMessage(long roleId, int channel, String content, long targetId) {
        // 检查禁言
        if (isMuted(roleId)) {
            return Result.fail(ErrorCode.CHAT_MUTED);
        }

        // 敏感词过滤 (简化处理，实际项目应使用专业敏感词库)
        String filteredContent = filterSensitiveWords(content);

        // 获取发送者信息
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (!playerResult.isSuccess()) {
            return Result.fail(playerResult.getCode(), playerResult.getMessage());
        }
        PlayerDTO sender = playerResult.getData();

        // 频道特定检查
        switch (channel) {
            case CHANNEL_WORLD -> {
                // 世界频道发言间隔限制
                String limitKey = WORLD_CHAT_LIMIT_KEY + roleId;
                if (redisService.exists(limitKey)) {
                    return Result.fail(ErrorCode.CHAT_TOO_FREQUENT);
                }
                redisService.setEx(limitKey, "1", WORLD_CHAT_INTERVAL);
            }
            case CHANNEL_GUILD -> {
                // 检查是否有公会
                if (sender.getGuildId() <= 0) {
                    return Result.fail(ErrorCode.GUILD_NOT_JOINED);
                }
                targetId = sender.getGuildId();
            }
            case CHANNEL_PRIVATE -> {
                // 检查目标玩家
                if (targetId <= 0) {
                    return Result.fail(ErrorCode.PARAM_ERROR);
                }
            }
            default -> {
                return Result.fail(ErrorCode.PARAM_ERROR);
            }
        }

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setMsgId(idGenerator.nextId());
        message.setChannel(channel);
        message.setSenderId(roleId);
        message.setSenderName(sender.getRoleName());
        message.setSenderLevel(sender.getLevel());
        message.setSenderAvatar(sender.getAvatarId());
        message.setSenderVip(sender.getVipLevel());
        message.setContent(filteredContent);
        message.setSendTime(System.currentTimeMillis());
        message.setTargetId(targetId);

        if (channel == CHANNEL_GUILD) {
            message.setGuildId(sender.getGuildId());
        }

        // 保存消息
        chatMessageRepository.save(message);

        // 推送消息 (通过 Redis Stream)
        publishMessage(message);

        log.info("聊天消息发送成功: roleId={}, channel={}, msgId={}", roleId, channel, message.getMsgId());
        return Result.success(message.getMsgId());
    }

    @Override
    public Result<List<ChatMessageDTO>> getHistory(long roleId, int channel, long targetId, long lastMsgId, int count) {
        if (count <= 0 || count > 100) {
            count = 50;
        }
        if (lastMsgId <= 0) {
            lastMsgId = Long.MAX_VALUE;
        }

        PageRequest pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "sendTime"));
        List<ChatMessage> messages;

        switch (channel) {
            case CHANNEL_WORLD, CHANNEL_SYSTEM -> {
                messages = chatMessageRepository.findByChannelAndMsgIdLessThan(channel, lastMsgId, pageable);
            }
            case CHANNEL_GUILD -> {
                // 获取玩家公会
                Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
                if (!playerResult.isSuccess() || playerResult.getData().getGuildId() <= 0) {
                    return Result.success(Collections.emptyList());
                }
                messages = chatMessageRepository.findGuildMessages(
                    playerResult.getData().getGuildId(), lastMsgId, pageable);
            }
            case CHANNEL_PRIVATE -> {
                if (targetId <= 0) {
                    return Result.fail(ErrorCode.PARAM_ERROR);
                }
                messages = chatMessageRepository.findPrivateMessages(roleId, targetId, lastMsgId, pageable);
            }
            default -> {
                return Result.fail(ErrorCode.PARAM_ERROR);
            }
        }

        List<ChatMessageDTO> result = messages.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<Void> mute(long roleId, long duration, String reason) {
        MuteInfo muteInfo = new MuteInfo();
        muteInfo.setRoleId(roleId);
        muteInfo.setMuteTime(System.currentTimeMillis());
        muteInfo.setReason(reason);

        if (duration <= 0) {
            muteInfo.setMuteEndTime(0); // 永久禁言
        } else {
            muteInfo.setMuteEndTime(System.currentTimeMillis() + duration * 1000);
        }

        muteInfoRepository.save(muteInfo);

        // 缓存禁言状态
        String key = MUTE_KEY + roleId;
        if (duration <= 0) {
            redisService.set(key, String.valueOf(muteInfo.getMuteEndTime()));
        } else {
            redisService.setEx(key, String.valueOf(muteInfo.getMuteEndTime()), duration);
        }

        log.info("玩家被禁言: roleId={}, duration={}, reason={}", roleId, duration, reason);
        return Result.success();
    }

    @Override
    public Result<Void> unmute(long roleId) {
        muteInfoRepository.deleteById(roleId);
        redisService.delete(MUTE_KEY + roleId);

        log.info("玩家解除禁言: roleId={}", roleId);
        return Result.success();
    }

    @Override
    public Result<Void> sendSystemNotice(int noticeType, String title, String content) {
        // 创建系统消息
        ChatMessage message = new ChatMessage();
        message.setMsgId(idGenerator.nextId());
        message.setChannel(CHANNEL_SYSTEM);
        message.setSenderId(0);
        message.setSenderName("系统");
        message.setContent(content);
        message.setSendTime(System.currentTimeMillis());

        chatMessageRepository.save(message);

        // 通过 MQ 广播系统公告
        ChatMqMessage mqMessage = new ChatMqMessage();
        mqMessage.setMsgId(message.getMsgId());
        mqMessage.setChannel(CHANNEL_SYSTEM);
        mqMessage.setSenderId(0);
        mqMessage.setSenderName("系统");
        mqMessage.setContent(content);
        mqMessage.setSendTime(message.getSendTime());
        mqMessage.addProperty("noticeType", String.valueOf(noticeType));
        mqMessage.addProperty("title", title);
        
        mqProducer.broadcast(MqExchange.CHAT_WORLD, mqMessage);

        log.info("系统公告发送: type={}, title={}", noticeType, title);
        return Result.success();
    }

    /**
     * 检查是否被禁言
     */
    private boolean isMuted(long roleId) {
        String key = MUTE_KEY + roleId;
        String muteEndStr = redisService.get(key);
        if (muteEndStr == null) {
            return false;
        }

        long muteEnd = Long.parseLong(muteEndStr);
        if (muteEnd == 0) {
            return true; // 永久禁言
        }
        return System.currentTimeMillis() < muteEnd;
    }

    /**
     * 过滤敏感词 (简化实现)
     */
    private String filterSensitiveWords(String content) {
        // 实际项目中应使用 DFA 算法或第三方敏感词库
        return content;
    }

    /**
     * 发布消息到 RabbitMQ
     * <p>
     * 替代原有的 Redis Pub/Sub，使用 RabbitMQ 减少 Redis 压力
     * </p>
     */
    private void publishMessage(ChatMessage message) {
        ChatMqMessage mqMessage = new ChatMqMessage();
        mqMessage.setMsgId(message.getMsgId());
        mqMessage.setChannel(message.getChannel());
        mqMessage.setSenderId(message.getSenderId());
        mqMessage.setSenderName(message.getSenderName());
        mqMessage.setSenderLevel(message.getSenderLevel());
        mqMessage.setSenderAvatar(message.getSenderAvatar());
        mqMessage.setSenderVip(message.getSenderVip());
        mqMessage.setContent(message.getContent());
        mqMessage.setTargetId(message.getTargetId());
        mqMessage.setGuildId(message.getGuildId());
        mqMessage.setSendTime(message.getSendTime());

        switch (message.getChannel()) {
            case CHANNEL_WORLD -> {
                // 世界聊天广播 (fanout)
                mqProducer.broadcast(MqExchange.CHAT_WORLD, mqMessage);
            }
            case CHANNEL_GUILD -> {
                // 公会聊天定向 (direct)
                mqProducer.send(MqExchange.CHAT_DIRECT, "guild." + message.getGuildId(), mqMessage);
            }
            case CHANNEL_PRIVATE -> {
                // 私聊定向 (direct)
                mqProducer.send(MqExchange.CHAT_DIRECT, "private." + message.getTargetId(), mqMessage);
            }
            default -> log.warn("未知的聊天频道: {}", message.getChannel());
        }
    }

    /**
     * 转换为 DTO
     */
    private ChatMessageDTO toDTO(ChatMessage entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
