package com.game.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 战斗消息
 * <p>
 * 用于战斗相关的 MQ 传输
 * </p>
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BattleMqMessage extends MqMessage {

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
     * 消息类型
     */
    private BattleMessageType type;

    /**
     * 参与者角色ID列表
     */
    private List<Long> participantIds;

    /**
     * 胜利者角色ID列表
     */
    private List<Long> winnerIds;

    /**
     * 奖励数据 (roleId -> rewards JSON)
     */
    private Map<Long, String> rewards;

    /**
     * 战斗数据 (JSON)
     */
    private String battleData;

    /**
     * 副本ID (PVE)
     */
    private int dungeonId;

    /**
     * 难度 (PVE)
     */
    private int difficulty;

    /**
     * 战斗持续时间 (毫秒)
     */
    private long duration;

    public BattleMqMessage() {
        super();
    }

    /**
     * 战斗消息类型
     */
    public enum BattleMessageType {
        /**
         * 战斗开始
         */
        BATTLE_START,
        
        /**
         * 战斗结束
         */
        BATTLE_END,
        
        /**
         * 战斗结算
         */
        BATTLE_SETTLE,
        
        /**
         * 战斗操作
         */
        BATTLE_ACTION,
        
        /**
         * 战斗同步
         */
        BATTLE_SYNC
    }
}
