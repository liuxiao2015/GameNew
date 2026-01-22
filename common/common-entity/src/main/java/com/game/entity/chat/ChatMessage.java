package com.game.entity.chat;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.CompoundIndex;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 聊天消息 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "chat_message")
@CompoundIndex(name = "idx_channel_time", def = "{\"channel\": 1, \"sendTime\": -1}")
@CompoundIndex(name = "idx_private_chat", def = "{\"senderId\": 1, \"targetId\": 1, \"sendTime\": -1}")
public class ChatMessage extends BaseDocument {

    /**
     * 消息 ID
     */
    @MongoIndex(unique = true)
    private long msgId;

    /**
     * 频道 (1:世界 2:公会 3:私聊 4:系统)
     */
    @MongoIndex
    private int channel;

    /**
     * 发送者 ID
     */
    @MongoIndex
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
    @MongoIndex
    private long sendTime;

    /**
     * 目标 ID (私聊时为对方 ID)
     */
    private long targetId;

    /**
     * 公会 ID (公会频道时)
     */
    private long guildId;

    /**
     * 消息类型 (0:文字 1:表情 2:语音)
     */
    private int msgType;

    /**
     * 额外数据 (JSON)
     */
    private String extraData;
}
