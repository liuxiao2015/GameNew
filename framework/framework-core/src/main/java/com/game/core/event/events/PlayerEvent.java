package com.game.core.event.events;

import com.game.core.event.BaseGameEvent;
import lombok.Getter;

/**
 * 玩家事件基类
 *
 * @author GameServer
 */
@Getter
public abstract class PlayerEvent extends BaseGameEvent {

    protected PlayerEvent(long roleId) {
        super(roleId);
    }

    // ==================== 常用玩家事件 ====================

    /**
     * 玩家登录事件
     */
    @Getter
    public static class PlayerLoginEvent extends PlayerEvent {
        private final boolean firstLogin;
        private final String ip;

        public PlayerLoginEvent(long roleId, boolean firstLogin, String ip) {
            super(roleId);
            this.firstLogin = firstLogin;
            this.ip = ip;
        }
    }

    /**
     * 玩家登出事件
     */
    @Getter
    public static class PlayerLogoutEvent extends PlayerEvent {
        private final long onlineDuration; // 在线时长 (秒)

        public PlayerLogoutEvent(long roleId, long onlineDuration) {
            super(roleId);
            this.onlineDuration = onlineDuration;
        }
    }

    /**
     * 玩家升级事件
     */
    @Getter
    public static class PlayerLevelUpEvent extends PlayerEvent {
        private final int oldLevel;
        private final int newLevel;

        public PlayerLevelUpEvent(long roleId, int oldLevel, int newLevel) {
            super(roleId);
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }
    }

    /**
     * 玩家数据变更事件
     */
    @Getter
    public static class PlayerDataChangeEvent extends PlayerEvent {
        private final String dataType; // 变更的数据类型 (如 "gold", "level")
        private final Object oldValue;
        private final Object newValue;

        public PlayerDataChangeEvent(long roleId, String dataType, Object oldValue, Object newValue) {
            super(roleId);
            this.dataType = dataType;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    /**
     * 货币变更事件
     */
    @Getter
    public static class CurrencyChangeEvent extends PlayerEvent {
        private final int currencyType;
        private final long oldAmount;
        private final long newAmount;
        private final String reason;

        public CurrencyChangeEvent(long roleId, int currencyType, long oldAmount, long newAmount, String reason) {
            super(roleId);
            this.currencyType = currencyType;
            this.oldAmount = oldAmount;
            this.newAmount = newAmount;
            this.reason = reason;
        }

        public long getDelta() {
            return newAmount - oldAmount;
        }
    }

    /**
     * 物品获得事件
     */
    @Getter
    public static class ItemAcquiredEvent extends PlayerEvent {
        private final int itemId;
        private final int count;
        private final String source;

        public ItemAcquiredEvent(long roleId, int itemId, int count, String source) {
            super(roleId);
            this.itemId = itemId;
            this.count = count;
            this.source = source;
        }
    }

    /**
     * 物品消耗事件
     */
    @Getter
    public static class ItemConsumedEvent extends PlayerEvent {
        private final int itemId;
        private final int count;
        private final String reason;

        public ItemConsumedEvent(long roleId, int itemId, int count, String reason) {
            super(roleId);
            this.itemId = itemId;
            this.count = count;
            this.reason = reason;
        }
    }

    /**
     * 任务完成事件
     */
    @Getter
    public static class QuestCompleteEvent extends PlayerEvent {
        private final int questId;

        public QuestCompleteEvent(long roleId, int questId) {
            super(roleId);
            this.questId = questId;
        }
    }

    /**
     * 成就解锁事件
     */
    @Getter
    public static class AchievementUnlockEvent extends PlayerEvent {
        private final int achievementId;

        public AchievementUnlockEvent(long roleId, int achievementId) {
            super(roleId);
            this.achievementId = achievementId;
        }
    }
}
