package com.game.service.guild.event;

import com.game.core.event.BaseGameEvent;
import lombok.Getter;

import java.util.List;

/**
 * 公会相关事件定义
 *
 * @author GameServer
 */
public final class GuildEvents {

    private GuildEvents() {}

    /**
     * 公会创建事件
     */
    @Getter
    public static class GuildCreatedEvent extends BaseGameEvent {
        private final long guildId;
        private final String guildName;
        private final long leaderId;
        private final String leaderName;

        public GuildCreatedEvent(long guildId, String guildName, long leaderId, String leaderName) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
        }
    }

    /**
     * 公会解散事件
     */
    @Getter
    public static class GuildDisbandedEvent extends BaseGameEvent {
        private final long guildId;
        private final String guildName;
        private final List<Long> memberIds;

        public GuildDisbandedEvent(long guildId, String guildName, List<Long> memberIds) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.memberIds = memberIds;
        }
    }

    /**
     * 成员加入公会事件
     */
    @Getter
    public static class MemberJoinedEvent extends BaseGameEvent {
        private final long guildId;
        private final String guildName;
        private final long roleId;
        private final String roleName;

        public MemberJoinedEvent(long guildId, String guildName, long roleId, String roleName) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.roleId = roleId;
            this.roleName = roleName;
        }
    }

    /**
     * 成员离开公会事件
     */
    @Getter
    public static class MemberLeftEvent extends BaseGameEvent {
        private final long guildId;
        private final String guildName;
        private final long roleId;
        private final LeaveType leaveType;

        public MemberLeftEvent(long guildId, String guildName, long roleId, LeaveType leaveType) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.roleId = roleId;
            this.leaveType = leaveType;
        }

        public enum LeaveType {
            LEAVE,      // 主动退出
            KICKED,     // 被踢出
            DISBANDED   // 公会解散
        }
    }

    /**
     * 公会升级事件
     */
    @Getter
    public static class GuildLevelUpEvent extends BaseGameEvent {
        private final long guildId;
        private final String guildName;
        private final int oldLevel;
        private final int newLevel;

        public GuildLevelUpEvent(long guildId, String guildName, int oldLevel, int newLevel) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }
    }

    /**
     * 公会捐献事件
     */
    @Getter
    public static class GuildDonateEvent extends BaseGameEvent {
        private final long guildId;
        private final long roleId;
        private final int donateType;
        private final long amount;
        private final long contribution;

        public GuildDonateEvent(long guildId, long roleId, int donateType, long amount, long contribution) {
            this.guildId = guildId;
            this.roleId = roleId;
            this.donateType = donateType;
            this.amount = amount;
            this.contribution = contribution;
        }
    }
}
