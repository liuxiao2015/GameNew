package com.game.service.battle.core;

import lombok.Data;

import java.util.List;

/**
 * 战斗操作记录
 * <p>
 * 用于记录战斗过程，支持回放
 * </p>
 *
 * @author GameServer
 */
@Data
public class BattleAction {

    /**
     * 操作序号
     */
    private int sequence;

    /**
     * 回合数
     */
    private int round;

    /**
     * 操作类型
     */
    private ActionType actionType;

    /**
     * 执行单位ID
     */
    private int sourceUnitId;

    /**
     * 目标单位ID列表
     */
    private List<Integer> targetUnitIds;

    /**
     * 技能ID
     */
    private int skillId;

    /**
     * 操作结果
     */
    private List<ActionResult> results;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 操作类型枚举
     */
    public enum ActionType {
        /**
         * 普通攻击
         */
        ATTACK,
        
        /**
         * 释放技能
         */
        SKILL,
        
        /**
         * Buff 生效
         */
        BUFF_TRIGGER,
        
        /**
         * 单位死亡
         */
        UNIT_DEATH,
        
        /**
         * 回合开始
         */
        ROUND_START,
        
        /**
         * 回合结束
         */
        ROUND_END,
        
        /**
         * 战斗开始
         */
        BATTLE_START,
        
        /**
         * 战斗结束
         */
        BATTLE_END
    }

    /**
     * 操作结果
     */
    @Data
    public static class ActionResult {
        /**
         * 目标单位ID
         */
        private int targetUnitId;
        
        /**
         * 结果类型
         */
        private ResultType resultType;
        
        /**
         * 数值变化
         */
        private long value;
        
        /**
         * 是否暴击
         */
        private boolean critical;
        
        /**
         * 是否闪避
         */
        private boolean dodged;
        
        /**
         * 附加的 Buff ID
         */
        private int buffId;

        /**
         * 结果类型枚举
         */
        public enum ResultType {
            DAMAGE,      // 伤害
            HEAL,        // 治疗
            SHIELD,      // 护盾
            BUFF_ADD,    // 添加Buff
            BUFF_REMOVE, // 移除Buff
            ENERGY,      // 能量变化
            MISS,        // 未命中
            IMMUNE       // 免疫
        }
    }
}
