package com.game.service.battle.core;

import lombok.Data;

import java.util.List;

/**
 * 技能定义
 *
 * @author GameServer
 */
@Data
public class Skill {

    /**
     * 技能ID
     */
    private int skillId;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能类型 (0:普攻 1:主动 2:被动 3:大招)
     */
    private int skillType;

    /**
     * 技能目标类型 (1:单体 2:群体 3:随机 4:全体)
     */
    private int targetType;

    /**
     * 技能目标阵营 (1:敌方 2:友方 3:自身 4:全部)
     */
    private int targetCamp;

    /**
     * 最大冷却回合
     */
    private int maxCooldown;

    /**
     * 当前冷却回合
     */
    private int currentCooldown;

    /**
     * 能量消耗
     */
    private int energyCost;

    /**
     * 技能效果列表
     */
    private List<SkillEffect> effects;

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return currentCooldown <= 0;
    }

    /**
     * 使用技能后进入冷却
     */
    public void enterCooldown() {
        this.currentCooldown = maxCooldown;
    }
}
