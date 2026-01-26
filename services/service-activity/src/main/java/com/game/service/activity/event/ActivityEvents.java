package com.game.service.activity.event;

import com.game.core.event.BaseGameEvent;
import lombok.Getter;

/**
 * 活动相关事件
 *
 * @author GameServer
 */
public class ActivityEvents {

    /**
     * 活动开始事件
     */
    @Getter
    public static class ActivityStartEvent extends BaseGameEvent {
        private final int activityId;
        private final String activityName;

        public ActivityStartEvent(int activityId, String activityName) {
            super(0);
            this.activityId = activityId;
            this.activityName = activityName;
        }
    }

    /**
     * 活动结束事件
     */
    @Getter
    public static class ActivityEndEvent extends BaseGameEvent {
        private final int activityId;
        private final String activityName;

        public ActivityEndEvent(int activityId, String activityName) {
            super(0);
            this.activityId = activityId;
            this.activityName = activityName;
        }
    }

    /**
     * 活动进度更新事件
     */
    @Getter
    public static class ProgressUpdateEvent extends BaseGameEvent {
        private final int activityId;
        private final String goalId;
        private final long oldProgress;
        private final long newProgress;

        public ProgressUpdateEvent(long roleId, int activityId, String goalId, 
                                    long oldProgress, long newProgress) {
            super(roleId);
            this.activityId = activityId;
            this.goalId = goalId;
            this.oldProgress = oldProgress;
            this.newProgress = newProgress;
        }
    }

    /**
     * 活动奖励领取事件
     */
    @Getter
    public static class RewardClaimedEvent extends BaseGameEvent {
        private final int activityId;
        private final String rewardId;

        public RewardClaimedEvent(long roleId, int activityId, String rewardId) {
            super(roleId);
            this.activityId = activityId;
            this.rewardId = rewardId;
        }
    }

    /**
     * 玩家参与活动事件
     */
    @Getter
    public static class ParticipateEvent extends BaseGameEvent {
        private final int activityId;
        private final int participateCount;

        public ParticipateEvent(long roleId, int activityId, int participateCount) {
            super(roleId);
            this.activityId = activityId;
            this.participateCount = participateCount;
        }
    }
}
