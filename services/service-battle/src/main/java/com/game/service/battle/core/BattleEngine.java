package com.game.service.battle.core;

import com.game.api.battle.BattleActionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗引擎
 * <p>
 * 负责战斗逻辑的执行和计算
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleEngine {

    /**
     * 活跃战斗缓存
     */
    private final Map<Long, BattleContext> activeBattles = new ConcurrentHashMap<>();

    /**
     * 创建战斗
     */
    public BattleContext createBattle(long battleId, int battleType, int maxRound, long timeoutMs) {
        BattleContext context = new BattleContext();
        context.setBattleId(battleId);
        context.setBattleType(battleType);
        context.setMaxRound(maxRound);
        context.setTimeoutMs(timeoutMs);
        context.init();
        
        activeBattles.put(battleId, context);
        log.info("创建战斗: battleId={}, type={}", battleId, battleType);
        return context;
    }

    /**
     * 获取战斗
     */
    public BattleContext getBattle(long battleId) {
        return activeBattles.get(battleId);
    }

    /**
     * 移除战斗
     */
    public void removeBattle(long battleId) {
        activeBattles.remove(battleId);
        log.info("移除战斗: battleId={}", battleId);
    }

    /**
     * 开始战斗
     */
    public void startBattle(BattleContext context) {
        context.start();
        
        // 记录战斗开始
        BattleAction startAction = new BattleAction();
        startAction.setActionType(BattleAction.ActionType.BATTLE_START);
        context.recordAction(startAction);
    }

    /**
     * 执行战斗操作
     */
    public List<BattleAction> executeAction(BattleContext context, BattleActionDTO actionDTO) {
        List<BattleAction> actions = new ArrayList<>();
        
        if (context.getStatus() != BattleContext.BattleStatus.IN_PROGRESS) {
            log.warn("战斗不在进行中: battleId={}, status={}", context.getBattleId(), context.getStatus());
            return actions;
        }
        
        BattleUnit currentUnit = context.getCurrentActionUnit();
        if (currentUnit == null) {
            return actions;
        }
        
        switch (actionDTO.getActionType()) {
            case ATTACK -> actions.addAll(executeAttack(context, currentUnit, actionDTO));
            case SKILL -> actions.addAll(executeSkill(context, currentUnit, actionDTO));
            case AUTO -> actions.addAll(executeAuto(context, currentUnit));
            case SKIP -> actions.addAll(executeSkip(context, currentUnit));
            case SURRENDER -> {
                // 投降
                context.end(currentUnit.getCamp() == 1 ? 2 : 1);
                BattleAction endAction = new BattleAction();
                endAction.setActionType(BattleAction.ActionType.BATTLE_END);
                endAction.setSourceUnitId(currentUnit.getUnitId());
                context.recordAction(endAction);
                actions.add(endAction);
            }
            default -> log.warn("未知操作类型: {}", actionDTO.getActionType());
        }
        
        // 检查战斗是否结束
        if (!context.checkBattleEnd()) {
            // 移动到下一个行动单位
            context.nextActionUnit();
        }
        
        return actions;
    }

    /**
     * 执行普通攻击
     */
    private List<BattleAction> executeAttack(BattleContext context, BattleUnit attacker, BattleActionDTO actionDTO) {
        List<BattleAction> actions = new ArrayList<>();
        
        Skill defaultSkill = attacker.getDefaultAttackSkill();
        if (defaultSkill == null) {
            log.warn("单位没有普攻技能: unitId={}", attacker.getUnitId());
            return actions;
        }
        
        // 获取目标
        List<BattleUnit> targets = getTargets(context, attacker, defaultSkill, actionDTO.getTargetIds());
        if (targets.isEmpty()) {
            return actions;
        }
        
        // 创建操作记录
        BattleAction action = new BattleAction();
        action.setActionType(BattleAction.ActionType.ATTACK);
        action.setSourceUnitId(attacker.getUnitId());
        action.setTargetUnitIds(targets.stream().map(BattleUnit::getUnitId).toList());
        action.setSkillId(defaultSkill.getSkillId());
        
        List<BattleAction.ActionResult> results = new ArrayList<>();
        
        for (BattleUnit target : targets) {
            BattleAction.ActionResult result = calculateDamage(context, attacker, target, defaultSkill);
            results.add(result);
            
            // 应用伤害
            if (result.getResultType() == BattleAction.ActionResult.ResultType.DAMAGE) {
                target.takeDamage(result.getValue(), attacker, context);
            }
        }
        
        action.setResults(results);
        context.recordAction(action);
        actions.add(action);
        
        // 攻击后增加能量
        attacker.addEnergy(10);
        
        return actions;
    }

    /**
     * 执行技能
     */
    private List<BattleAction> executeSkill(BattleContext context, BattleUnit caster, BattleActionDTO actionDTO) {
        List<BattleAction> actions = new ArrayList<>();
        
        Skill skill = caster.getSkills().stream()
                .filter(s -> s.getSkillId() == actionDTO.getSkillId())
                .findFirst()
                .orElse(null);
        
        if (skill == null || !skill.isAvailable()) {
            log.warn("技能不可用: unitId={}, skillId={}", caster.getUnitId(), actionDTO.getSkillId());
            return actions;
        }
        
        if (!caster.consumeEnergy(skill.getEnergyCost())) {
            log.warn("能量不足: unitId={}, required={}, current={}", 
                    caster.getUnitId(), skill.getEnergyCost(), caster.getEnergy());
            return actions;
        }
        
        // 获取目标
        List<BattleUnit> targets = getTargets(context, caster, skill, actionDTO.getTargetIds());
        
        // 创建操作记录
        BattleAction action = new BattleAction();
        action.setActionType(BattleAction.ActionType.SKILL);
        action.setSourceUnitId(caster.getUnitId());
        action.setTargetUnitIds(targets.stream().map(BattleUnit::getUnitId).toList());
        action.setSkillId(skill.getSkillId());
        
        List<BattleAction.ActionResult> results = new ArrayList<>();
        
        // 处理技能效果
        for (SkillEffect effect : skill.getEffects()) {
            for (BattleUnit target : targets) {
                BattleAction.ActionResult result = applySkillEffect(context, caster, target, effect);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        
        action.setResults(results);
        context.recordAction(action);
        actions.add(action);
        
        // 技能进入冷却
        skill.enterCooldown();
        
        return actions;
    }

    /**
     * 执行自动战斗 (AI)
     */
    private List<BattleAction> executeAuto(BattleContext context, BattleUnit unit) {
        // 简单 AI: 优先使用可用技能，否则普攻
        List<Skill> availableSkills = unit.getAvailableSkills();
        
        BattleActionDTO autoAction = new BattleActionDTO();
        
        if (!availableSkills.isEmpty() && availableSkills.stream().anyMatch(s -> s.getSkillType() > 0)) {
            // 使用第一个非普攻的可用技能
            Skill skill = availableSkills.stream()
                    .filter(s -> s.getSkillType() > 0)
                    .findFirst()
                    .orElse(availableSkills.get(0));
            autoAction.setActionType(BattleActionDTO.ActionType.SKILL);
            autoAction.setSkillId(skill.getSkillId());
        } else {
            autoAction.setActionType(BattleActionDTO.ActionType.ATTACK);
        }
        
        autoAction.setUnitId(unit.getUnitId());
        return executeAction(context, autoAction);
    }

    /**
     * 跳过回合
     */
    private List<BattleAction> executeSkip(BattleContext context, BattleUnit unit) {
        List<BattleAction> actions = new ArrayList<>();
        
        BattleAction action = new BattleAction();
        action.setActionType(BattleAction.ActionType.ROUND_END);
        action.setSourceUnitId(unit.getUnitId());
        context.recordAction(action);
        actions.add(action);
        
        return actions;
    }

    /**
     * 获取技能目标
     */
    private List<BattleUnit> getTargets(BattleContext context, BattleUnit caster, 
                                         Skill skill, List<Integer> targetIds) {
        List<BattleUnit> result = new ArrayList<>();
        Map<Integer, BattleUnit> targetPool;
        
        // 根据技能目标阵营确定目标池
        switch (skill.getTargetCamp()) {
            case 1 -> targetPool = caster.getCamp() == 1 ? context.getEnemyUnits() : context.getMyUnits();
            case 2 -> targetPool = caster.getCamp() == 1 ? context.getMyUnits() : context.getEnemyUnits();
            case 3 -> {
                result.add(caster);
                return result;
            }
            default -> targetPool = context.getAllUnits();
        }
        
        List<BattleUnit> aliveTargets = targetPool.values().stream()
                .filter(BattleUnit::isAlive)
                .toList();
        
        if (aliveTargets.isEmpty()) {
            return result;
        }
        
        // 根据技能目标类型选择目标
        switch (skill.getTargetType()) {
            case 1 -> {
                // 单体
                if (targetIds != null && !targetIds.isEmpty()) {
                    BattleUnit target = targetPool.get(targetIds.get(0));
                    if (target != null && target.isAlive()) {
                        result.add(target);
                    }
                }
                if (result.isEmpty()) {
                    result.add(aliveTargets.get(context.nextInt(aliveTargets.size())));
                }
            }
            case 2 -> {
                // 群体 (前排/后排)
                result.addAll(aliveTargets);
            }
            case 3 -> {
                // 随机 (随机选择一个)
                result.add(aliveTargets.get(context.nextInt(aliveTargets.size())));
            }
            case 4 -> {
                // 全体
                result.addAll(aliveTargets);
            }
        }
        
        return result;
    }

    /**
     * 计算伤害
     */
    private BattleAction.ActionResult calculateDamage(BattleContext context, BattleUnit attacker, 
                                                       BattleUnit target, Skill skill) {
        BattleAction.ActionResult result = new BattleAction.ActionResult();
        result.setTargetUnitId(target.getUnitId());
        
        // 命中判定
        int hitChance = attacker.getHitRate() - target.getDodgeRate();
        if (context.nextInt(10000) >= hitChance) {
            result.setResultType(BattleAction.ActionResult.ResultType.MISS);
            result.setDodged(true);
            return result;
        }
        
        // 基础伤害 = 攻击力 * 技能系数
        long baseDamage = attacker.getAttack();
        
        // 暴击判定
        boolean isCrit = context.nextInt(10000) < attacker.getCritRate();
        if (isCrit) {
            baseDamage = baseDamage * attacker.getCritDamage() / 10000;
            result.setCritical(true);
        }
        
        // 伤害浮动 (±5%)
        int fluctuation = context.nextInt(-500, 500);
        baseDamage = baseDamage * (10000 + fluctuation) / 10000;
        
        result.setResultType(BattleAction.ActionResult.ResultType.DAMAGE);
        result.setValue(Math.max(1, baseDamage));
        
        return result;
    }

    /**
     * 应用技能效果
     */
    private BattleAction.ActionResult applySkillEffect(BattleContext context, BattleUnit caster, 
                                                        BattleUnit target, SkillEffect effect) {
        BattleAction.ActionResult result = new BattleAction.ActionResult();
        result.setTargetUnitId(target.getUnitId());
        
        // 概率判定
        if (effect.getProbability() > 0 && context.nextInt(10000) >= effect.getProbability()) {
            return null;
        }
        
        switch (effect.getType()) {
            case DAMAGE -> {
                long damage = effect.isPercentage() 
                        ? caster.getAttack() * effect.getValue() / 10000 
                        : effect.getValue();
                
                // 暴击判定
                if (context.nextInt(10000) < caster.getCritRate()) {
                    damage = damage * caster.getCritDamage() / 10000;
                    result.setCritical(true);
                }
                
                target.takeDamage(damage, caster, context);
                result.setResultType(BattleAction.ActionResult.ResultType.DAMAGE);
                result.setValue(damage);
            }
            case HEAL -> {
                long heal = effect.isPercentage() 
                        ? target.getMaxHp() * effect.getValue() / 10000 
                        : effect.getValue();
                long actualHeal = target.heal(heal);
                result.setResultType(BattleAction.ActionResult.ResultType.HEAL);
                result.setValue(actualHeal);
            }
            case ADD_BUFF -> {
                Buff buff = new Buff();
                buff.setBuffId(effect.getBuffId());
                buff.setDuration(effect.getBuffDuration());
                buff.setStackCount(1);
                target.addBuff(buff);
                result.setResultType(BattleAction.ActionResult.ResultType.BUFF_ADD);
                result.setBuffId(effect.getBuffId());
            }
            case ADD_ENERGY -> {
                int energy = (int) effect.getValue();
                target.addEnergy(energy);
                result.setResultType(BattleAction.ActionResult.ResultType.ENERGY);
                result.setValue(energy);
            }
            default -> {
                return null;
            }
        }
        
        return result;
    }

    /**
     * 获取活跃战斗数量
     */
    public int getActiveBattleCount() {
        return activeBattles.size();
    }
}
