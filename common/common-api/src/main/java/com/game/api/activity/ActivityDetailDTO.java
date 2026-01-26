package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 活动详情 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动基本信息
     */
    private ActivityDTO activity;

    /**
     * 活动目标列表
     */
    private List<ActivityGoalDTO> goals;

    /**
     * 活动奖励列表
     */
    private List<ActivityRewardDTO> rewards;

    /**
     * 玩家进度数据
     */
    private PlayerActivityDTO playerData;

    /**
     * 活动规则说明
     */
    private String rules;

    /**
     * 活动目标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityGoalDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 目标ID
         */
        private String goalId;

        /**
         * 目标名称
         */
        private String name;

        /**
         * 目标描述
         */
        private String description;

        /**
         * 目标值
         */
        private long targetValue;

        /**
         * 当前进度
         */
        private long currentValue;

        /**
         * 是否已完成
         */
        private boolean completed;
    }

    /**
     * 活动奖励
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityRewardDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 奖励ID
         */
        private String rewardId;

        /**
         * 奖励名称
         */
        private String name;

        /**
         * 关联目标ID
         */
        private String goalId;

        /**
         * 需要达到的进度
         */
        private long requiredProgress;

        /**
         * 奖励内容列表
         */
        private List<RewardDTO> rewards;

        /**
         * 是否可领取
         */
        private boolean canClaim;

        /**
         * 是否已领取
         */
        private boolean claimed;
    }
}
