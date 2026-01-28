package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 战斗单位 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUnitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单位ID (战斗内唯一)
     */
    private int unitId;

    /**
     * 角色ID (玩家单位) 或 怪物模板ID (AI单位)
     */
    private long entityId;

    /**
     * 单位名称
     */
    private String name;

    /**
     * 单位类型 (1:玩家 2:怪物 3:召唤物 4:NPC)
     */
    private int unitType;

    /**
     * 阵营 (1:我方 2:敌方)
     */
    private int camp;

    /**
     * 位置索引
     */
    private int position;

    /**
     * 当前生命值
     */
    private long hp;

    /**
     * 最大生命值
     */
    private long maxHp;

    /**
     * 当前能量/怒气
     */
    private int energy;

    /**
     * 最大能量
     */
    private int maxEnergy;

    /**
     * 攻击力
     */
    private long attack;

    /**
     * 防御力
     */
    private long defense;

    /**
     * 速度
     */
    private int speed;

    /**
     * 暴击率 (万分比)
     */
    private int critRate;

    /**
     * 暴击伤害 (万分比)
     */
    private int critDamage;

    /**
     * 技能列表
     */
    private List<SkillDTO> skills;

    /**
     * Buff 列表
     */
    private List<BuffDTO> buffs;

    /**
     * 是否存活
     */
    private boolean alive;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;

    /**
     * 技能 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int skillId;
        private String skillName;
        private int skillType;      // 1:主动 2:被动 3:大招
        private int cooldown;       // 当前冷却
        private int maxCooldown;    // 最大冷却
        private int energyCost;     // 能量消耗
        private boolean available;  // 是否可用
    }

    /**
     * Buff DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuffDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int buffId;
        private String buffName;
        private int buffType;       // 1:增益 2:减益
        private int stackCount;     // 层数
        private int duration;       // 剩余回合
        private long value;         // 效果值
    }
}
