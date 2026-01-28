package com.game.service.battle.core;

import lombok.Data;

/**
 * 技能效果
 *
 * @author GameServer
 */
@Data
public class SkillEffect {

    /**
     * 效果类型
     */
    private EffectType type;

    /**
     * 效果值 (万分比或固定值)
     */
    private long value;

    /**
     * 是否百分比
     */
    private boolean percentage;

    /**
     * 概率 (万分比)
     */
    private int probability;

    /**
     * 附加 Buff ID
     */
    private int buffId;

    /**
     * Buff 持续回合
     */
    private int buffDuration;

    /**
     * 效果类型枚举
     */
    public enum EffectType {
        /**
         * 伤害
         */
        DAMAGE,
        
        /**
         * 治疗
         */
        HEAL,
        
        /**
         * 护盾
         */
        SHIELD,
        
        /**
         * 添加 Buff
         */
        ADD_BUFF,
        
        /**
         * 移除 Buff
         */
        REMOVE_BUFF,
        
        /**
         * 驱散
         */
        DISPEL,
        
        /**
         * 增加能量
         */
        ADD_ENERGY,
        
        /**
         * 减少能量
         */
        REDUCE_ENERGY,
        
        /**
         * 复活
         */
        REVIVE,
        
        /**
         * 召唤
         */
        SUMMON
    }
}
