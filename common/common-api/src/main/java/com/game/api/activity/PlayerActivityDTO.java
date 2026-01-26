package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 玩家活动数据 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerActivityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private int activityId;

    /**
     * 活动版本
     */
    private int version;

    /**
     * 进度数据 (目标ID -> 进度值)
     */
    private Map<String, Long> progress;

    /**
     * 已领取的奖励ID集合
     */
    private Set<String> claimedRewards;

    /**
     * 累计参与次数
     */
    private int participateCount;

    /**
     * 今日参与次数
     */
    private int todayCount;

    /**
     * 最后参与时间
     */
    private long lastParticipateTime;

    /**
     * 是否有可领取的奖励
     */
    private boolean hasClaimableReward;
}
