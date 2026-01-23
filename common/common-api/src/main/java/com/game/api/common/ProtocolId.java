package com.game.api.common;

/**
 * 协议 ID 定义
 * <p>
 * 协议号 = 模块号 + 方法号
 * 例如: 0x0101 = 登录模块(0x01) + 握手方法(0x01)
 * </p>
 * <p>
 * 协议号规划:
 * <pre>
 *   0x0100 - 0x01FF : 登录模块 (256个协议)
 *   0x0200 - 0x02FF : 玩家模块 (256个协议)
 *   0x0300 - 0x03FF : 背包模块 (256个协议)
 *   0x0400 - 0x04FF : 装备模块 (256个协议)
 *   0x0500 - 0x05FF : 任务模块 (256个协议)
 *   0x0600 - 0x06FF : 公会模块 (256个协议)
 *   0x0700 - 0x07FF : 聊天模块 (256个协议)
 *   0x0800 - 0x08FF : 排行模块 (256个协议)
 *   0x0900 - 0x09FF : 活动模块 (256个协议)
 *   0x0A00 - 0x0AFF : 邮件模块 (256个协议)
 *   0x0B00 - 0x0BFF : 好友模块 (256个协议)
 *   0x0C00 - 0x0CFF : 副本模块 (256个协议)
 *   0x0D00 - 0x0DFF : 商城模块 (256个协议)
 *   0xF000 - 0xFFFF : 推送消息 (4096个协议)
 * </pre>
 * </p>
 *
 * @author GameServer
 */
public final class ProtocolId {

    private ProtocolId() {}

    // =====================================================================================
    //                              登录模块 (0x0100 - 0x01FF)
    // =====================================================================================

    /** 握手 */
    public static final int LOGIN_HANDSHAKE = 0x0101;
    /** 账号登录 */
    public static final int LOGIN_ACCOUNT_LOGIN = 0x0102;
    /** 绑定账号 */
    public static final int LOGIN_BIND_ACCOUNT = 0x0103;
    /** 获取服务器列表 */
    public static final int LOGIN_GET_SERVER_LIST = 0x0104;
    /** 选择服务器 */
    public static final int LOGIN_SELECT_SERVER = 0x0105;
    /** 检查角色名 */
    public static final int LOGIN_CHECK_ROLE_NAME = 0x0106;
    /** 创建角色 */
    public static final int LOGIN_CREATE_ROLE = 0x0107;
    /** 进入游戏 */
    public static final int LOGIN_ENTER_GAME = 0x0108;
    /** 登出 */
    public static final int LOGIN_LOGOUT = 0x0109;
    /** 重连 */
    public static final int LOGIN_RECONNECT = 0x010A;
    /** 心跳 */
    public static final int LOGIN_HEARTBEAT = 0x010B;

    // =====================================================================================
    //                              玩家模块 (0x0200 - 0x02FF)
    // =====================================================================================

    /** 获取玩家信息 */
    public static final int PLAYER_GET_INFO = 0x0201;
    /** 更新玩家信息 */
    public static final int PLAYER_UPDATE_INFO = 0x0202;
    /** 修改名字 */
    public static final int PLAYER_CHANGE_NAME = 0x0203;
    /** 获取背包 */
    public static final int PLAYER_GET_BAG = 0x0210;
    /** 使用物品 */
    public static final int PLAYER_USE_ITEM = 0x0211;
    /** 出售物品 */
    public static final int PLAYER_SELL_ITEM = 0x0212;
    /** 整理背包 */
    public static final int PLAYER_ARRANGE_BAG = 0x0213;

    // =====================================================================================
    //                              公会模块 (0x0600 - 0x06FF)
    // =====================================================================================

    /** 创建公会 */
    public static final int GUILD_CREATE = 0x0601;
    /** 获取公会信息 */
    public static final int GUILD_GET_INFO = 0x0602;
    /** 搜索公会 */
    public static final int GUILD_SEARCH = 0x0603;
    /** 申请加入公会 */
    public static final int GUILD_APPLY_JOIN = 0x0604;
    /** 处理加入申请 */
    public static final int GUILD_HANDLE_APPLY = 0x0605;
    /** 退出公会 */
    public static final int GUILD_LEAVE = 0x0606;
    /** 踢出成员 */
    public static final int GUILD_KICK_MEMBER = 0x0607;
    /** 公会捐献 */
    public static final int GUILD_DONATE = 0x0608;
    /** 修改成员职位 */
    public static final int GUILD_CHANGE_POSITION = 0x0609;
    /** 转让会长 */
    public static final int GUILD_TRANSFER_LEADER = 0x060A;
    /** 修改公会设置 */
    public static final int GUILD_CHANGE_SETTING = 0x060B;

    // =====================================================================================
    //                              聊天模块 (0x0700 - 0x07FF)
    // =====================================================================================

    /** 发送聊天消息 */
    public static final int CHAT_SEND = 0x0701;
    /** 获取聊天记录 */
    public static final int CHAT_GET_HISTORY = 0x0702;

    // =====================================================================================
    //                              排行模块 (0x0800 - 0x08FF)
    // =====================================================================================

    /** 获取排行榜 */
    public static final int RANK_GET_LIST = 0x0801;
    /** 获取自己排名 */
    public static final int RANK_GET_MY_RANK = 0x0802;

    // =====================================================================================
    //                              推送消息 (0xF000 - 0xFFFF)
    // =====================================================================================

    /** 踢下线 */
    public static final int PUSH_KICK_OUT = 0xF001;
    /** 玩家属性变化 */
    public static final int PUSH_PLAYER_ATTR_CHANGE = 0xF002;
    /** 升级 */
    public static final int PUSH_LEVEL_UP = 0xF003;
    /** 物品变化 */
    public static final int PUSH_ITEM_CHANGE = 0xF004;
    /** 聊天消息推送 */
    public static final int PUSH_CHAT = 0xF010;
    /** 系统公告 */
    public static final int PUSH_SYSTEM_NOTICE = 0xF011;
    /** 公会成员变化 */
    public static final int PUSH_GUILD_MEMBER_CHANGE = 0xF020;
    /** 公会信息变化 */
    public static final int PUSH_GUILD_INFO_CHANGE = 0xF021;
    /** 公会申请 */
    public static final int PUSH_GUILD_APPLY = 0xF022;
    /** 新邮件 */
    public static final int PUSH_NEW_MAIL = 0xF030;
    /** 好友请求 */
    public static final int PUSH_FRIEND_REQUEST = 0xF040;
    /** 好友上线 */
    public static final int PUSH_FRIEND_ONLINE = 0xF041;
    /** 好友下线 */
    public static final int PUSH_FRIEND_OFFLINE = 0xF042;

    // =====================================================================================
    //                              辅助方法
    // =====================================================================================

    /**
     * 获取模块号 (高 8 位)
     */
    public static int getModuleId(int protocolId) {
        return (protocolId >> 8) & 0xFF;
    }

    /**
     * 获取方法号 (低 8 位)
     */
    public static int getMethodId(int protocolId) {
        return protocolId & 0xFF;
    }

    /**
     * 组合协议号
     */
    public static int makeProtocolId(int moduleId, int methodId) {
        return (moduleId << 8) | (methodId & 0xFF);
    }

    /**
     * 是否是推送消息
     */
    public static boolean isPush(int protocolId) {
        return protocolId >= 0xF000;
    }

    /**
     * 是否是登录模块
     */
    public static boolean isLoginModule(int protocolId) {
        return getModuleId(protocolId) == 0x01;
    }

    /**
     * 是否是玩家模块
     */
    public static boolean isPlayerModule(int protocolId) {
        return getModuleId(protocolId) == 0x02;
    }

    /**
     * 是否是公会模块
     */
    public static boolean isGuildModule(int protocolId) {
        return getModuleId(protocolId) == 0x06;
    }
}
