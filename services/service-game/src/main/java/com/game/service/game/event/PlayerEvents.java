package com.game.service.game.event;

import com.game.core.event.GameEvent;
import lombok.Getter;

/**
 * 玩家相关事件定义
 * <p>
 * 所有玩家相关的事件都在这里定义，其他服务可以监听这些事件来实现解耦的业务逻辑
 * </p>
 *
 * @author GameServer
 */
public final class PlayerEvents {

    private PlayerEvents() {}

    // ==================== 登录相关事件 ====================

    /**
     * 玩家登录事件
     */
    @Getter
    public static class PlayerLoginEvent extends GameEvent {
        private final long roleId;
        private final String roleName;
        private final int level;
        private final String sessionId;
        private final int serverId;

        public PlayerLoginEvent(long roleId, String roleName, int level, String sessionId, int serverId) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.level = level;
            this.sessionId = sessionId;
            this.serverId = serverId;
        }
    }

    /**
     * 玩家登出事件
     */
    @Getter
    public static class PlayerLogoutEvent extends GameEvent {
        private final long roleId;
        private final String roleName;
        private final long onlineTime;

        public PlayerLogoutEvent(long roleId, String roleName, long onlineTime) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.onlineTime = onlineTime;
        }
    }

    // ==================== 升级相关事件 ====================

    /**
     * 玩家升级事件
     */
    @Getter
    public static class PlayerLevelUpEvent extends GameEvent {
        private final long roleId;
        private final int oldLevel;
        private final int newLevel;

        public PlayerLevelUpEvent(long roleId, int oldLevel, int newLevel) {
            this.roleId = roleId;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }
    }

    /**
     * VIP 升级事件
     */
    @Getter
    public static class VipLevelUpEvent extends GameEvent {
        private final long roleId;
        private final int oldVipLevel;
        private final int newVipLevel;

        public VipLevelUpEvent(long roleId, int oldVipLevel, int newVipLevel) {
            this.roleId = roleId;
            this.oldVipLevel = oldVipLevel;
            this.newVipLevel = newVipLevel;
        }
    }

    // ==================== 货币变化事件 ====================

    /**
     * 金币变化事件
     */
    @Getter
    public static class GoldChangeEvent extends GameEvent {
        private final long roleId;
        private final long oldValue;
        private final long newValue;
        private final String reason;

        public GoldChangeEvent(long roleId, long oldValue, long newValue, String reason) {
            this.roleId = roleId;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.reason = reason;
        }
    }

    /**
     * 钻石变化事件
     */
    @Getter
    public static class DiamondChangeEvent extends GameEvent {
        private final long roleId;
        private final long oldValue;
        private final long newValue;
        private final String reason;

        public DiamondChangeEvent(long roleId, long oldValue, long newValue, String reason) {
            this.roleId = roleId;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.reason = reason;
        }
    }

    // ==================== 战力变化事件 ====================

    /**
     * 战力变化事件
     */
    @Getter
    public static class CombatPowerChangeEvent extends GameEvent {
        private final long roleId;
        private final long oldPower;
        private final long newPower;
        private final String source;

        public CombatPowerChangeEvent(long roleId, long oldPower, long newPower, String source) {
            this.roleId = roleId;
            this.oldPower = oldPower;
            this.newPower = newPower;
            this.source = source;
        }
    }

    // ==================== 公会相关事件 ====================

    /**
     * 玩家加入公会事件
     */
    @Getter
    public static class PlayerJoinGuildEvent extends GameEvent {
        private final long roleId;
        private final long guildId;
        private final String guildName;

        public PlayerJoinGuildEvent(long roleId, long guildId, String guildName) {
            this.roleId = roleId;
            this.guildId = guildId;
            this.guildName = guildName;
        }
    }

    /**
     * 玩家离开公会事件
     */
    @Getter
    public static class PlayerLeaveGuildEvent extends GameEvent {
        private final long roleId;
        private final long guildId;
        private final String guildName;
        private final LeaveReason reason;

        public PlayerLeaveGuildEvent(long roleId, long guildId, String guildName, LeaveReason reason) {
            this.roleId = roleId;
            this.guildId = guildId;
            this.guildName = guildName;
            this.reason = reason;
        }

        public enum LeaveReason {
            LEAVE,      // 主动退出
            KICKED,     // 被踢出
            DISBANDED   // 公会解散
        }
    }

    // ==================== 物品相关事件 ====================

    /**
     * 物品获得事件
     */
    @Getter
    public static class ItemObtainEvent extends GameEvent {
        private final long roleId;
        private final int itemId;
        private final long count;
        private final String reason;

        public ItemObtainEvent(long roleId, int itemId, long count, String reason) {
            this.roleId = roleId;
            this.itemId = itemId;
            this.count = count;
            this.reason = reason;
        }
    }

    /**
     * 物品消耗事件
     */
    @Getter
    public static class ItemConsumeEvent extends GameEvent {
        private final long roleId;
        private final int itemId;
        private final long count;
        private final String reason;

        public ItemConsumeEvent(long roleId, int itemId, long count, String reason) {
            this.roleId = roleId;
            this.itemId = itemId;
            this.count = count;
            this.reason = reason;
        }
    }
}
