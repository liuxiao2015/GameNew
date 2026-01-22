package com.game.service.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 聊天消息实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "chat_message")
@CompoundIndexes({
    @CompoundIndex(name = "idx_channel_time", def = "{'channel': 1, 'sendTime': -1}"),
    @CompoundIndex(name = "idx_private_chat", def = "{'senderId': 1, 'targetId': 1, 'sendTime': -1}")
})
public class ChatMessage {

    /**
     * 消息 ID
     */
    @Id
    private long msgId;

    /**
     * 频道 (1:世界 2:公会 3:私聊 4:系统)
     */
    @Indexed
    private int channel;

    /**
     * 发送者 ID
     */
    private long senderId;

    /**
     * 发送者名字
     */
    private String senderName;

    /**
     * 发送者等级
     */
    private int senderLevel;

    /**
     * 发送者头像
     */
    private int senderAvatar;

    /**
     * 发送者 VIP
     */
    private int senderVip;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    @Indexed
    private long sendTime;

    /**
     * 目标 ID (私聊时为接收者, 公会时为公会 ID)
     */
    private long targetId;

    /**
     * 目标名字
     */
    private String targetName;

    /**
     * 公会 ID (公会频道使用)
     */
    private long guildId;
}
