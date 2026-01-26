package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 玩家排名 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRankDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private int activityId;

    /**
     * 玩家排名 (0=未上榜)
     */
    private int rank;

    /**
     * 玩家分数
     */
    private long score;

    /**
     * 总参与人数
     */
    private long totalCount;

    /**
     * 距离上一名的差距
     */
    private long gapToNext;

    /**
     * 预计可获得的奖励
     */
    private String expectedReward;
}
