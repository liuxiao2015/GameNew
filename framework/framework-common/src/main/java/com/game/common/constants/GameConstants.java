package com.game.common.constants;

/**
 * 游戏常量定义
 *
 * @author GameServer
 */
public final class GameConstants {

    private GameConstants() {
        // 禁止实例化
    }

    // ==================== 系统常量 ====================

    /**
     * 系统用户 ID (用于系统邮件、公告等)
     */
    public static final long SYSTEM_USER_ID = 0L;

    /**
     * 默认页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== Redis Key 前缀 ====================

    /**
     * 玩家数据缓存前缀
     */
    public static final String REDIS_PLAYER_PREFIX = "player:";

    /**
     * 公会数据缓存前缀
     */
    public static final String REDIS_GUILD_PREFIX = "guild:";

    /**
     * 玩家 Token 前缀
     */
    public static final String REDIS_TOKEN_PREFIX = "token:";

    /**
     * 在线玩家集合
     */
    public static final String REDIS_ONLINE_SET = "online:players";

    /**
     * 排行榜前缀
     */
    public static final String REDIS_RANK_PREFIX = "rank:";

    /**
     * 配置缓存前缀
     */
    public static final String REDIS_CONFIG_PREFIX = "config:";

    /**
     * 分布式锁前缀
     */
    public static final String REDIS_LOCK_PREFIX = "lock:";

    /**
     * Redis Stream 前缀
     */
    public static final String REDIS_STREAM_PREFIX = "stream:";

    // ==================== MongoDB 集合名 ====================

    /**
     * 玩家数据集合
     */
    public static final String MONGO_PLAYER_COLLECTION = "player_data";

    /**
     * 公会数据集合
     */
    public static final String MONGO_GUILD_COLLECTION = "guild_data";

    /**
     * 操作日志集合
     */
    public static final String MONGO_OPERATION_LOG_COLLECTION = "operation_log";

    /**
     * 邮件集合
     */
    public static final String MONGO_MAIL_COLLECTION = "mail";

    /**
     * 聊天记录集合
     */
    public static final String MONGO_CHAT_COLLECTION = "chat_log";

    // ==================== Actor 常量 ====================

    /**
     * Actor 空闲过期时间 (毫秒)
     */
    public static final long ACTOR_IDLE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * Actor 数据保存间隔 (毫秒)
     */
    public static final long ACTOR_SAVE_INTERVAL_MS = 5 * 60 * 1000L;

    /**
     * Actor 消息队列最大容量
     */
    public static final int ACTOR_MAILBOX_MAX_SIZE = 10000;

    // ==================== 协议常量 ====================

    /**
     * 协议头长度 (包长度字段本身)
     */
    public static final int PROTOCOL_HEADER_LENGTH = 4;

    /**
     * 最大包长度
     */
    public static final int PROTOCOL_MAX_FRAME_LENGTH = 1024 * 1024;

    /**
     * 心跳间隔 (秒)
     */
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    /**
     * 心跳超时 (秒)
     */
    public static final int HEARTBEAT_TIMEOUT_SECONDS = 90;

    // ==================== 时间常量 ====================

    /**
     * Token 过期时间 (小时)
     */
    public static final int TOKEN_EXPIRE_HOURS = 24 * 7;

    /**
     * 验证码过期时间 (分钟)
     */
    public static final int VERIFY_CODE_EXPIRE_MINUTES = 5;

    /**
     * 日志保留天数
     */
    public static final int LOG_RETENTION_DAYS = 30;
}
