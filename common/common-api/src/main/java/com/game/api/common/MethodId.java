package com.game.api.common;

/**
 * 方法 ID 定义
 * <p>
 * 用于 @Protocol 注解的 methodId 参数
 * 完整协议号 = 模块号 (高 8 位) + 方法号 (低 8 位)
 * </p>
 *
 * @author GameServer
 */
public final class MethodId {

    private MethodId() {}

    // =====================================================================================
    //                              登录模块方法 (模块号: 0x01)
    // =====================================================================================

    public static final class Login {
        private Login() {}

        /** 握手 */
        public static final int HANDSHAKE = 0x01;
        /** 账号登录 */
        public static final int ACCOUNT_LOGIN = 0x02;
        /** 绑定账号 */
        public static final int BIND_ACCOUNT = 0x03;
        /** 获取服务器列表 */
        public static final int GET_SERVER_LIST = 0x04;
        /** 选择服务器 */
        public static final int SELECT_SERVER = 0x05;
        /** 检查角色名 */
        public static final int CHECK_ROLE_NAME = 0x06;
        /** 创建角色 */
        public static final int CREATE_ROLE = 0x07;
        /** 进入游戏 */
        public static final int ENTER_GAME = 0x08;
        /** 登出 */
        public static final int LOGOUT = 0x09;
        /** 重连 */
        public static final int RECONNECT = 0x0A;
        /** 心跳 */
        public static final int HEARTBEAT = 0x0B;
    }

    // =====================================================================================
    //                              玩家模块方法 (模块号: 0x02)
    // =====================================================================================

    public static final class Player {
        private Player() {}

        /** 获取玩家信息 */
        public static final int GET_INFO = 0x01;
        /** 更新玩家信息 */
        public static final int UPDATE_INFO = 0x02;
        /** 修改名字 */
        public static final int CHANGE_NAME = 0x03;

        /** 获取背包 */
        public static final int GET_BAG = 0x10;
        /** 使用物品 */
        public static final int USE_ITEM = 0x11;
        /** 出售物品 */
        public static final int SELL_ITEM = 0x12;
        /** 整理背包 */
        public static final int ARRANGE_BAG = 0x13;
    }

    // =====================================================================================
    //                              背包模块方法 (模块号: 0x03)
    // =====================================================================================

    public static final class Bag {
        private Bag() {}

        /** 获取背包列表 */
        public static final int GET_LIST = 0x01;
        /** 使用物品 */
        public static final int USE_ITEM = 0x02;
        /** 出售物品 */
        public static final int SELL_ITEM = 0x03;
        /** 丢弃物品 */
        public static final int DROP_ITEM = 0x04;
        /** 整理背包 */
        public static final int ARRANGE = 0x05;
        /** 分解物品 */
        public static final int DECOMPOSE = 0x06;
    }

    // =====================================================================================
    //                              装备模块方法 (模块号: 0x04)
    // =====================================================================================

    public static final class Equipment {
        private Equipment() {}

        /** 穿戴装备 */
        public static final int EQUIP = 0x01;
        /** 卸下装备 */
        public static final int UNEQUIP = 0x02;
        /** 强化装备 */
        public static final int ENHANCE = 0x03;
        /** 升星 */
        public static final int STAR_UP = 0x04;
        /** 镶嵌宝石 */
        public static final int INLAY_GEM = 0x05;
        /** 拆卸宝石 */
        public static final int REMOVE_GEM = 0x06;
    }

    // =====================================================================================
    //                              任务模块方法 (模块号: 0x05)
    // =====================================================================================

    public static final class Quest {
        private Quest() {}

        /** 获取任务列表 */
        public static final int GET_LIST = 0x01;
        /** 接取任务 */
        public static final int ACCEPT = 0x02;
        /** 提交任务 */
        public static final int SUBMIT = 0x03;
        /** 放弃任务 */
        public static final int ABANDON = 0x04;
        /** 领取奖励 */
        public static final int CLAIM_REWARD = 0x05;
    }

    // =====================================================================================
    //                              公会模块方法 (模块号: 0x06)
    // =====================================================================================

    public static final class Guild {
        private Guild() {}

        /** 创建公会 */
        public static final int CREATE = 0x01;
        /** 获取公会信息 */
        public static final int GET_INFO = 0x02;
        /** 搜索公会 */
        public static final int SEARCH = 0x03;
        /** 申请加入公会 */
        public static final int APPLY_JOIN = 0x04;
        /** 处理加入申请 */
        public static final int HANDLE_APPLY = 0x05;
        /** 退出公会 */
        public static final int LEAVE = 0x06;
        /** 踢出成员 */
        public static final int KICK_MEMBER = 0x07;
        /** 公会捐献 */
        public static final int DONATE = 0x08;
        /** 修改成员职位 */
        public static final int CHANGE_POSITION = 0x09;
        /** 转让会长 */
        public static final int TRANSFER_LEADER = 0x0A;
        /** 修改公会设置 */
        public static final int CHANGE_SETTING = 0x0B;
    }

    // =====================================================================================
    //                              聊天模块方法 (模块号: 0x07)
    // =====================================================================================

    public static final class Chat {
        private Chat() {}

        /** 发送聊天消息 */
        public static final int SEND = 0x01;
        /** 获取聊天记录 */
        public static final int GET_HISTORY = 0x02;
    }

    // =====================================================================================
    //                              排行模块方法 (模块号: 0x08)
    // =====================================================================================

    public static final class Rank {
        private Rank() {}

        /** 获取排行榜 */
        public static final int GET_LIST = 0x01;
        /** 获取自己排名 */
        public static final int GET_MY_RANK = 0x02;
    }

    // =====================================================================================
    //                              活动模块方法 (模块号: 0x09)
    // =====================================================================================

    public static final class Activity {
        private Activity() {}

        /** 获取活动列表 */
        public static final int GET_LIST = 0x01;
        /** 获取活动详情 */
        public static final int GET_DETAIL = 0x02;
        /** 领取活动奖励 */
        public static final int CLAIM_REWARD = 0x03;
    }

    // =====================================================================================
    //                              邮件模块方法 (模块号: 0x0A)
    // =====================================================================================

    public static final class Mail {
        private Mail() {}

        /** 获取邮件列表 */
        public static final int GET_LIST = 0x01;
        /** 读取邮件 */
        public static final int READ = 0x02;
        /** 领取附件 */
        public static final int CLAIM_ATTACHMENT = 0x03;
        /** 删除邮件 */
        public static final int DELETE = 0x04;
        /** 一键领取 */
        public static final int CLAIM_ALL = 0x05;
    }

    // =====================================================================================
    //                              好友模块方法 (模块号: 0x0B)
    // =====================================================================================

    public static final class Friend {
        private Friend() {}

        /** 获取好友列表 */
        public static final int GET_LIST = 0x01;
        /** 添加好友 */
        public static final int ADD = 0x02;
        /** 删除好友 */
        public static final int DELETE = 0x03;
        /** 处理好友请求 */
        public static final int HANDLE_REQUEST = 0x04;
        /** 搜索玩家 */
        public static final int SEARCH_PLAYER = 0x05;
    }

    // =====================================================================================
    //                              副本模块方法 (模块号: 0x0C)
    // =====================================================================================

    public static final class Dungeon {
        private Dungeon() {}

        /** 获取副本列表 */
        public static final int GET_LIST = 0x01;
        /** 进入副本 */
        public static final int ENTER = 0x02;
        /** 退出副本 */
        public static final int EXIT = 0x03;
        /** 扫荡副本 */
        public static final int SWEEP = 0x04;
        /** 领取奖励 */
        public static final int CLAIM_REWARD = 0x05;
    }

    // =====================================================================================
    //                              商城模块方法 (模块号: 0x0D)
    // =====================================================================================

    public static final class Shop {
        private Shop() {}

        /** 获取商城列表 */
        public static final int GET_LIST = 0x01;
        /** 购买商品 */
        public static final int BUY = 0x02;
        /** 刷新商城 */
        public static final int REFRESH = 0x03;
    }
}
