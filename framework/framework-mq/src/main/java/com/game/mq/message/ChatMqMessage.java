package com.game.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天消息
 * <p>
 * 用于聊天消息的 MQ 传输
 * </p>
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMqMessage extends MqMessage {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private long msgId;

    /**
     * 频道类型 (1:世界 2:公会 3:私聊 4:系统)
     */
    private int channel;

    /**
     * 发送者ID
     */
    private long senderId;

    /**
     * 发送者名称
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
     * 发送者VIP等级
     */
    private int senderVip;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 目标ID (私聊目标/公会ID)
     */
    private long targetId;

    /**
     * 公会ID
     */
    private long guildId;

    /**
     * 发送时间
     */
    private long sendTime;

    public ChatMqMessage() {
        super();
    }
}
