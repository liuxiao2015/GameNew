package com.game.entity.repository;

import com.game.entity.document.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息仓库
 *
 * @author GameServer
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByChannelAndMsgIdLessThan(int channel, long msgId, Pageable pageable);

    @Query("{'guildId': ?0, 'msgId': {$lt: ?1}}")
    List<ChatMessage> findGuildMessages(long guildId, long msgId, Pageable pageable);

    @Query("{'$or': [{'senderId': ?0, 'targetId': ?1}, {'senderId': ?1, 'targetId': ?0}], 'msgId': {$lt: ?2}}")
    List<ChatMessage> findPrivateMessages(long roleId, long targetId, long msgId, Pageable pageable);

    List<ChatMessage> findBySenderId(long senderId, Pageable pageable);
}
