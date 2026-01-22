package com.game.service.chat.repository;

import com.game.service.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * 聊天消息仓库
 *
 * @author GameServer
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessage, Long> {

    /**
     * 获取频道消息 (世界, 公会)
     */
    @Query("{ 'channel': ?0, 'msgId': { $lt: ?1 } }")
    List<ChatMessage> findByChannelAndMsgIdLessThan(int channel, long lastMsgId, Pageable pageable);

    /**
     * 获取公会消息
     */
    @Query("{ 'channel': 2, 'guildId': ?0, 'msgId': { $lt: ?1 } }")
    List<ChatMessage> findGuildMessages(long guildId, long lastMsgId, Pageable pageable);

    /**
     * 获取私聊消息
     */
    @Query("{ 'channel': 3, $or: [ { 'senderId': ?0, 'targetId': ?1 }, { 'senderId': ?1, 'targetId': ?0 } ], 'msgId': { $lt: ?2 } }")
    List<ChatMessage> findPrivateMessages(long roleId, long targetId, long lastMsgId, Pageable pageable);

    /**
     * 获取频道最新消息
     */
    List<ChatMessage> findByChannelOrderBySendTimeDesc(int channel, Pageable pageable);
}
