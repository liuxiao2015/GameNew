package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 战斗结果 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 战斗ID
     */
    private long battleId;

    /**
     * 战斗类型
     */
    private int battleType;

    /**
     * 是否胜利
     */
    private boolean victory;

    /**
     * 结果类型 (1:正常结束 2:超时 3:投降 4:掉线)
     */
    private int resultType;

    /**
     * 战斗时长 (毫秒)
     */
    private long duration;

    /**
     * 总回合数
     */
    private int totalRounds;

    /**
     * 星级评价 (1-3星, PVE)
     */
    private int stars;

    /**
     * 获得经验
     */
    private long expGain;

    /**
     * 获得金币
     */
    private long goldGain;

    /**
     * 积分变化 (PVP)
     */
    private int scoreChange;

    /**
     * 掉落道具
     */
    private List<RewardItem> drops;

    /**
     * 首通奖励 (首次通关)
     */
    private List<RewardItem> firstClearRewards;

    /**
     * 战斗统计
     */
    private BattleStatistics statistics;

    /**
     * 战斗回放数据 (JSON)
     */
    private String replayData;

    /**
     * 奖励道具
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int itemId;
        private String itemName;
        private long count;
        private int quality;    // 品质
    }

    /**
     * 战斗统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BattleStatistics implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 各单位伤害统计
         */
        private Map<Integer, Long> damageDealt;
        
        /**
         * 各单位承受伤害
         */
        private Map<Integer, Long> damageTaken;
        
        /**
         * 各单位治疗量
         */
        private Map<Integer, Long> healingDone;
        
        /**
         * 击杀数
         */
        private Map<Integer, Integer> kills;
        
        /**
         * MVP 单位ID
         */
        private int mvpUnitId;
    }
}
