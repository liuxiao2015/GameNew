package com.game.service.battle.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战斗单位
 * <p>
 * 战斗中的基本实体，可以是玩家、怪物、召唤物等
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Data
public class BattleUnit {

    /**
     * 单位ID (战斗内唯一)
     */
    private int unitId;

    /**
     * 实体ID (角色ID/怪物模板ID)
     */
    private long entityId;

    /**
     * 名称
     */
    private String name;

    /**
     * 单位类型 (1:玩家 2:怪物 3:召唤物)
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

    // ==================== 基础属性 ====================

    /**
     * 当前生命值
     */
    private long hp;

    /**
     * 最大生命值
     */
    private long maxHp;

    /**
     * 当前能量
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
     * 暴击伤害 (万分比, 基础 15000 = 150%)
     */
    private int critDamage;

    /**
     * 命中率 (万分比)
     */
    private int hitRate;

    /**
     * 闪避率 (万分比)
     */
    private int dodgeRate;

    // ==================== 技能与Buff ====================

    /**
     * 技能列表
     */
    private List<Skill> skills = new ArrayList<>();

    /**
     * Buff 列表
     */
    private List<Buff> buffs = new ArrayList<>();

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes = new HashMap<>();

    // ==================== 方法 ====================

    /**
     * 是否存活
     */
    public boolean isAlive() {
        return hp > 0;
    }

    /**
     * 受到伤害
     */
    public long takeDamage(long damage, BattleUnit attacker, BattleContext context) {
        if (!isAlive()) {
            return 0;
        }

        // 计算实际伤害
        long actualDamage = calculateActualDamage(damage, attacker);
        
        // 扣除生命值
        hp = Math.max(0, hp - actualDamage);
        
        log.debug("单位受到伤害: unitId={}, name={}, damage={}, remainHp={}", 
                unitId, name, actualDamage, hp);
        
        // 检查死亡
        if (!isAlive()) {
            onDeath(attacker, context);
        }
        
        return actualDamage;
    }

    /**
     * 计算实际伤害
     */
    private long calculateActualDamage(long baseDamage, BattleUnit attacker) {
        // 基础减伤公式: 实际伤害 = 基础伤害 * (1 - 防御 / (防御 + 常数))
        double damageReduction = (double) defense / (defense + 1000);
        long actualDamage = (long) (baseDamage * (1 - damageReduction));
        
        // 最低伤害为1
        return Math.max(1, actualDamage);
    }

    /**
     * 治疗
     */
    public long heal(long amount) {
        if (!isAlive()) {
            return 0;
        }
        
        long oldHp = hp;
        hp = Math.min(maxHp, hp + amount);
        long actualHeal = hp - oldHp;
        
        log.debug("单位治疗: unitId={}, name={}, heal={}, hp={}", 
                unitId, name, actualHeal, hp);
        
        return actualHeal;
    }

    /**
     * 增加能量
     */
    public void addEnergy(int amount) {
        energy = Math.min(maxEnergy, energy + amount);
    }

    /**
     * 消耗能量
     */
    public boolean consumeEnergy(int amount) {
        if (energy >= amount) {
            energy -= amount;
            return true;
        }
        return false;
    }

    /**
     * 添加 Buff
     */
    public void addBuff(Buff buff) {
        // 检查是否已有同类 Buff
        Buff existing = buffs.stream()
                .filter(b -> b.getBuffId() == buff.getBuffId())
                .findFirst()
                .orElse(null);
        
        if (existing != null) {
            // 刷新持续时间和叠加层数
            existing.setDuration(Math.max(existing.getDuration(), buff.getDuration()));
            existing.setStackCount(Math.min(existing.getMaxStack(), existing.getStackCount() + buff.getStackCount()));
        } else {
            buffs.add(buff);
        }
        
        log.debug("添加Buff: unitId={}, buffId={}, name={}", unitId, buff.getBuffId(), buff.getName());
    }

    /**
     * 移除 Buff
     */
    public void removeBuff(int buffId) {
        buffs.removeIf(b -> b.getBuffId() == buffId);
    }

    /**
     * 回合开始时调用
     */
    public void onRoundStart() {
        // 处理 Buff 效果
        List<Buff> expiredBuffs = new ArrayList<>();
        for (Buff buff : buffs) {
            buff.onRoundStart(this);
            buff.setDuration(buff.getDuration() - 1);
            if (buff.getDuration() <= 0) {
                expiredBuffs.add(buff);
            }
        }
        buffs.removeAll(expiredBuffs);
        
        // 技能冷却 -1
        skills.forEach(skill -> {
            if (skill.getCurrentCooldown() > 0) {
                skill.setCurrentCooldown(skill.getCurrentCooldown() - 1);
            }
        });
    }

    /**
     * 死亡时调用
     */
    private void onDeath(BattleUnit killer, BattleContext context) {
        log.info("单位死亡: unitId={}, name={}, killer={}", unitId, name, 
                killer != null ? killer.getName() : "unknown");
        // 可以在这里触发死亡相关效果
    }

    /**
     * 获取可用技能
     */
    public List<Skill> getAvailableSkills() {
        return skills.stream()
                .filter(skill -> skill.getCurrentCooldown() <= 0)
                .filter(skill -> skill.getEnergyCost() <= energy)
                .toList();
    }

    /**
     * 获取默认攻击技能
     */
    public Skill getDefaultAttackSkill() {
        return skills.stream()
                .filter(skill -> skill.getSkillType() == 0) // 普通攻击
                .findFirst()
                .orElse(null);
    }
}
