package com.game.api.chat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天消息 DTO
 *
 * @author GameServer
 */
@Data
public class ChatMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private long msgId;

    /**
     * 频道 (1:世界 2:公会 3:私聊 4:系统)
     */
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
     * 发送者 VIP 等级
     */
    private int senderVip;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private long sendTime;

    /**
     * 目标 ID (私聊时)
     */
    private long targetId;

    /**
     * 目标名字 (私聊时)
     */
    private String targetName;
}
