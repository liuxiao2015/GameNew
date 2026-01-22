package com.game.common.protocol;

/**
 * 协议常量定义
 *
 * @author GameServer
 */
public final class ProtocolConstants {

    private ProtocolConstants() {
        // 禁止实例化
    }

    // ==================== 协议头定义 ====================

    /**
     * 包长度字段长度
     */
    public static final int LENGTH_FIELD_LENGTH = 4;

    /**
     * 请求序号字段长度
     */
    public static final int SEQ_FIELD_LENGTH = 4;

    /**
     * 协议号字段长度
     */
    public static final int PROTOCOL_FIELD_LENGTH = 2;

    /**
     * 方法号字段长度
     */
    public static final int METHOD_FIELD_LENGTH = 2;

    /**
     * 错误码字段长度
     */
    public static final int ERROR_CODE_FIELD_LENGTH = 4;

    /**
     * 请求头总长度 (包长度 + 序号 + 协议号 + 方法号)
     */
    public static final int REQUEST_HEADER_LENGTH = LENGTH_FIELD_LENGTH + SEQ_FIELD_LENGTH 
            + PROTOCOL_FIELD_LENGTH + METHOD_FIELD_LENGTH;

    /**
     * 响应头总长度 (包长度 + 序号 + 错误码)
     */
    public static final int RESPONSE_HEADER_LENGTH = LENGTH_FIELD_LENGTH + SEQ_FIELD_LENGTH 
            + ERROR_CODE_FIELD_LENGTH;

    /**
     * 推送头总长度 (包长度 + 推送类型 + 协议号)
     */
    public static final int PUSH_HEADER_LENGTH = LENGTH_FIELD_LENGTH + PROTOCOL_FIELD_LENGTH 
            + PROTOCOL_FIELD_LENGTH;

    /**
     * 最大包长度 (1MB)
     */
    public static final int MAX_FRAME_LENGTH = 1024 * 1024;

    // ==================== 协议号范围 ====================

    /**
     * 登录协议起始
     */
    public static final int LOGIN_PROTOCOL_START = 1000;

    /**
     * 登录协议结束
     */
    public static final int LOGIN_PROTOCOL_END = 1999;

    /**
     * 玩家协议起始
     */
    public static final int PLAYER_PROTOCOL_START = 2000;

    /**
     * 玩家协议结束
     */
    public static final int PLAYER_PROTOCOL_END = 2999;

    /**
     * 背包协议起始
     */
    public static final int INVENTORY_PROTOCOL_START = 3000;

    /**
     * 背包协议结束
     */
    public static final int INVENTORY_PROTOCOL_END = 3999;

    /**
     * 装备协议起始
     */
    public static final int EQUIPMENT_PROTOCOL_START = 4000;

    /**
     * 装备协议结束
     */
    public static final int EQUIPMENT_PROTOCOL_END = 4999;

    /**
     * 任务协议起始
     */
    public static final int QUEST_PROTOCOL_START = 5000;

    /**
     * 任务协议结束
     */
    public static final int QUEST_PROTOCOL_END = 5999;

    /**
     * 公会协议起始
     */
    public static final int GUILD_PROTOCOL_START = 6000;

    /**
     * 公会协议结束
     */
    public static final int GUILD_PROTOCOL_END = 6999;

    /**
     * 聊天协议起始
     */
    public static final int CHAT_PROTOCOL_START = 7000;

    /**
     * 聊天协议结束
     */
    public static final int CHAT_PROTOCOL_END = 7999;

    /**
     * 排行协议起始
     */
    public static final int RANK_PROTOCOL_START = 8000;

    /**
     * 排行协议结束
     */
    public static final int RANK_PROTOCOL_END = 8999;

    /**
     * 系统协议起始
     */
    public static final int SYSTEM_PROTOCOL_START = 9000;

    /**
     * 系统协议结束
     */
    public static final int SYSTEM_PROTOCOL_END = 9999;

    // ==================== 特殊协议号 ====================

    /**
     * 心跳请求
     */
    public static final int HEARTBEAT_REQ = 1006;

    /**
     * 心跳响应
     */
    public static final int HEARTBEAT_RESP = 1006;

    /**
     * 心跳命令号 (兼容网关使用)
     */
    public static final int CMD_HEARTBEAT = HEARTBEAT_REQ;

    // ==================== 模块号 (用于路由) ====================

    /**
     * 登录模块
     */
    public static final int MODULE_LOGIN = 1;

    /**
     * 玩家模块
     */
    public static final int MODULE_PLAYER = 2;

    /**
     * 背包模块
     */
    public static final int MODULE_INVENTORY = 3;

    /**
     * 装备模块
     */
    public static final int MODULE_EQUIPMENT = 4;

    /**
     * 任务模块
     */
    public static final int MODULE_QUEST = 5;

    /**
     * 公会模块
     */
    public static final int MODULE_GUILD = 6;

    /**
     * 聊天模块
     */
    public static final int MODULE_CHAT = 7;

    /**
     * 排行模块
     */
    public static final int MODULE_RANK = 8;

    /**
     * 系统模块
     */
    public static final int MODULE_SYSTEM = 9;

    // ==================== 判断方法 ====================

    /**
     * 判断是否是登录相关协议
     */
    public static boolean isLoginProtocol(int protocolId) {
        return protocolId >= LOGIN_PROTOCOL_START && protocolId <= LOGIN_PROTOCOL_END;
    }

    /**
     * 判断是否是心跳协议
     */
    public static boolean isHeartbeatProtocol(int protocolId) {
        return protocolId == HEARTBEAT_REQ;
    }

    /**
     * 判断是否需要登录才能访问
     */
    public static boolean requireLogin(int protocolId) {
        // 登录相关协议不需要登录
        if (isLoginProtocol(protocolId)) {
            return false;
        }
        // 心跳协议不需要登录
        return !isHeartbeatProtocol(protocolId);
    }
}
