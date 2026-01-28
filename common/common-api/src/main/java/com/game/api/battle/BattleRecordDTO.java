package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 战斗记录 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 战斗ID
     */
    private long battleId;

    /**
     * 战斗类型 (1:PVE 2:PVP竞技 3:PVP匹配)
     */
    private int battleType;

    /**
     * 战斗类型名称
     */
    private String battleTypeName;

    /**
     * 副本ID (PVE)
     */
    private int dungeonId;

    /**
     * 副本名称 (PVE)
     */
    private String dungeonName;

    /**
     * 难度
     */
    private int difficulty;

    /**
     * 是否胜利
     */
    private boolean victory;

    /**
     * 星级 (PVE)
     */
    private int stars;

    /**
     * 积分变化 (PVP)
     */
    private int scoreChange;

    /**
     * 战斗时间
     */
    private long battleTime;

    /**
     * 战斗时长 (秒)
     */
    private int duration;

    /**
     * 对手信息 (PVP)
     */
    private List<OpponentInfo> opponents;

    /**
     * 是否有回放
     */
    private boolean hasReplay;

    /**
     * 对手信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpponentInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private long roleId;
        private String roleName;
        private int level;
        private int avatarId;
    }
}
