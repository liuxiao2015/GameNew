package com.game.service.battle.core;

import lombok.Data;

/**
 * Buff 定义
 *
 * @author GameServer
 */
@Data
public class Buff {

    /**
     * Buff ID
     */
    private int buffId;

    /**
     * Buff 名称
     */
    private String name;

    /**
     * Buff 类型 (1:增益 2:减益)
     */
    private int buffType;

    /**
     * 效果类型
     */
    private BuffEffectType effectType;

    /**
     * 效果值 (万分比或固定值)
     */
    private long value;

    /**
     * 是否百分比
     */
    private boolean percentage;

    /**
     * 剩余持续回合
     */
    private int duration;

    /**
     * 当前层数
     */
    private int stackCount;

    /**
     * 最大层数
     */
    private int maxStack;

    /**
     * 是否可驱散
     */
    private boolean dispellable;

    /**
     * 回合开始时触发
     */
    public void onRoundStart(BattleUnit unit) {
        switch (effectType) {
            case DOT -> {
                // 持续伤害
                long damage = calculateValue(unit);
                unit.setHp(Math.max(0, unit.getHp() - damage));
            }
            case HOT -> {
                // 持续治疗
                long heal = calculateValue(unit);
                unit.heal(heal);
            }
            default -> {
                // 其他类型在属性计算时生效
            }
        }
    }

    /**
     * 计算效果值
     */
    private long calculateValue(BattleUnit unit) {
        long baseValue = value * stackCount;
        if (percentage) {
            // 百分比计算，基于对应属性
            return switch (effectType) {
                case DOT -> unit.getMaxHp() * baseValue / 10000;
                case HOT -> unit.getMaxHp() * baseValue / 10000;
                default -> baseValue;
            };
        }
        return baseValue;
    }

    /**
     * Buff 效果类型
     */
    public enum BuffEffectType {
        /**
         * 攻击力增加
         */
        ATK_UP,
        
        /**
         * 攻击力降低
         */
        ATK_DOWN,
        
        /**
         * 防御力增加
         */
        DEF_UP,
        
        /**
         * 防御力降低
         */
        DEF_DOWN,
        
        /**
         * 速度增加
         */
        SPD_UP,
        
        /**
         * 速度降低
         */
        SPD_DOWN,
        
        /**
         * 暴击率增加
         */
        CRIT_UP,
        
        /**
         * 暴击率降低
         */
        CRIT_DOWN,
        
        /**
         * 持续伤害
         */
        DOT,
        
        /**
         * 持续治疗
         */
        HOT,
        
        /**
         * 护盾
         */
        SHIELD,
        
        /**
         * 眩晕
         */
        STUN,
        
        /**
         * 冰冻
         */
        FREEZE,
        
        /**
         * 沉默
         */
        SILENCE,
        
        /**
         * 无敌
         */
        INVINCIBLE,
        
        /**
         * 嘲讽
         */
        TAUNT
    }
}
