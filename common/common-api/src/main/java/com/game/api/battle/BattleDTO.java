package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 战斗信息 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 战斗ID
     */
    private long battleId;

    /**
     * 战斗类型 (1:PVE 2:PVP竞技 3:PVP匹配 4:公会战)
     */
    private int battleType;

    /**
     * 战斗状态 (0:准备 1:进行中 2:已结束)
     */
    private int status;

    /**
     * 当前回合
     */
    private int currentRound;

    /**
     * 最大回合数
     */
    private int maxRound;

    /**
     * 副本ID (PVE)
     */
    private int dungeonId;

    /**
     * 难度
     */
    private int difficulty;

    /**
     * 我方单位列表
     */
    private List<BattleUnitDTO> myUnits;

    /**
     * 敌方单位列表
     */
    private List<BattleUnitDTO> enemyUnits;

    /**
     * 战斗开始时间
     */
    private long startTime;

    /**
     * 战斗超时时间 (秒)
     */
    private int timeout;

    /**
     * 随机种子 (用于战斗重现)
     */
    private long randomSeed;
}
