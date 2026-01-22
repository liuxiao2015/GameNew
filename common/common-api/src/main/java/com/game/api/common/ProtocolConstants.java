package com.game.api.common;

/**
 * 协议常量定义
 * <p>
 * 协议格式：[总长度(4)] + [协议号(2)] + [方法号(2)] + [序列号(4)] + [数据体]
 * </p>
 *
 * @author GameServer
 */
public final class ProtocolConstants {

    private ProtocolConstants() {}

    // ==================== 协议头长度 ====================

    /**
     * 总长度字段长度
     */
    public static final int LENGTH_FIELD_LENGTH = 4;

    /**
     * 协议号字段长度
     */
    public static final int PROTOCOL_ID_LENGTH = 2;

    /**
     * 方法号字段长度
     */
    public static final int METHOD_ID_LENGTH = 2;

    /**
     * 序列号字段长度
     */
    public static final int SEQ_ID_LENGTH = 4;

    /**
     * 协议头总长度 (不含总长度字段)
     */
    public static final int HEADER_LENGTH = PROTOCOL_ID_LENGTH + METHOD_ID_LENGTH + SEQ_ID_LENGTH;

    /**
     * 最大消息长度
     */
    public static final int MAX_FRAME_LENGTH = 1024 * 1024; // 1MB

    // ==================== 协议号范围 ====================

    /**
     * 系统协议 (0x0001 - 0x00FF)
     */
    public static final int PROTOCOL_SYSTEM = 0x0001;

    /**
     * 登录协议 (0x0100 - 0x01FF)
     */
    public static final int PROTOCOL_LOGIN = 0x0100;

    /**
     * 玩家协议 (0x0200 - 0x02FF)
     */
    public static final int PROTOCOL_PLAYER = 0x0200;

    /**
     * 背包协议 (0x0300 - 0x03FF)
     */
    public static final int PROTOCOL_BAG = 0x0300;

    /**
     * 公会协议 (0x0400 - 0x04FF)
     */
    public static final int PROTOCOL_GUILD = 0x0400;

    /**
     * 聊天协议 (0x0500 - 0x05FF)
     */
    public static final int PROTOCOL_CHAT = 0x0500;

    /**
     * 排行协议 (0x0600 - 0x06FF)
     */
    public static final int PROTOCOL_RANK = 0x0600;

    /**
     * 任务协议 (0x0700 - 0x07FF)
     */
    public static final int PROTOCOL_QUEST = 0x0700;

    /**
     * 战斗协议 (0x0800 - 0x08FF)
     */
    public static final int PROTOCOL_BATTLE = 0x0800;

    // ==================== 系统消息 ID ====================

    /**
     * 心跳请求/响应
     */
    public static final int HEARTBEAT = 0x0001;

    /**
     * 踢出连接
     */
    public static final int KICK = 0x0002;

    /**
     * 系统公告
     */
    public static final int SYSTEM_NOTICE = 0x0003;

    // ==================== 消息类型 ====================

    /**
     * 请求消息
     */
    public static final byte MSG_TYPE_REQUEST = 0;

    /**
     * 响应消息
     */
    public static final byte MSG_TYPE_RESPONSE = 1;

    /**
     * 推送消息
     */
    public static final byte MSG_TYPE_PUSH = 2;
}
