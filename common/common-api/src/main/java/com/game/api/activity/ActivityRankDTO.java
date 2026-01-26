package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 活动排行榜 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRankDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private int activityId;

    /**
     * 排行榜列表
     */
    private List<RankEntryDTO> rankList;

    /**
     * 总参与人数
     */
    private long totalCount;

    /**
     * 排行条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankEntryDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 排名
         */
        private int rank;

        /**
         * 角色ID
         */
        private long roleId;

        /**
         * 角色名
         */
        private String roleName;

        /**
         * 服务器ID
         */
        private int serverId;

        /**
         * 分数
         */
        private long score;

        /**
         * 额外数据 (头像、等级等)
         */
        private String extra;
    }
}
