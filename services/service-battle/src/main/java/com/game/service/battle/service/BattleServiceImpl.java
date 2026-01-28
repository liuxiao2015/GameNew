package com.game.service.battle.service;

import com.game.api.battle.*;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.mq.MqExchange;
import com.game.mq.message.BattleMqMessage;
import com.game.mq.producer.MqProducer;
import com.game.service.battle.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 战斗服务实现
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class BattleServiceImpl implements BattleService {

    private final BattleEngine battleEngine;
    private final MqProducer mqProducer;

    @DubboReference(check = false)
    private PlayerService playerService;

    @Value("${game.battle.timeout-seconds:300}")
    private int battleTimeoutSeconds;

    @Value("${game.battle.max-rounds:30}")
    private int maxRounds;

    /**
     * 战斗ID生成器
     */
    private final AtomicLong battleIdGenerator = new AtomicLong(System.currentTimeMillis());

    /**
     * 玩家战斗映射 (roleId -> battleId)
     */
    private final Map<Long, Long> playerBattleMap = new ConcurrentHashMap<>();

    @Override
    public Result<BattleDTO> startPveBattle(long roleId, int dungeonId, int difficulty) {
        // 检查玩家是否已在战斗中
        if (playerBattleMap.containsKey(roleId)) {
            return Result.fail(ErrorCode.PARAM_ERROR, "已在战斗中");
        }

        // 创建战斗
        long battleId = battleIdGenerator.incrementAndGet();
        BattleContext context = battleEngine.createBattle(battleId, 1, maxRounds, 
                battleTimeoutSeconds * 1000L);
        context.setDungeonId(dungeonId);
        context.setDifficulty(difficulty);
        context.getParticipantIds().add(roleId);

        // 初始化玩家单位 (实际项目中应从玩家数据加载)
        initPlayerUnits(context, roleId);

        // 初始化敌方单位 (根据副本配置)
        initDungeonEnemies(context, dungeonId, difficulty);

        // 开始战斗
        battleEngine.startBattle(context);
        playerBattleMap.put(roleId, battleId);

        // 发送战斗开始消息
        sendBattleMessage(context, BattleMqMessage.BattleMessageType.BATTLE_START);

        log.info("PVE战斗开始: roleId={}, battleId={}, dungeonId={}", roleId, battleId, dungeonId);
        return Result.success(toBattleDTO(context));
    }

    @Override
    public Result<List<BattleResultDTO>> sweepDungeon(long roleId, int dungeonId, int difficulty, int count) {
        // 扫荡逻辑 (简化实现)
        List<BattleResultDTO> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BattleResultDTO result = BattleResultDTO.builder()
                    .battleId(battleIdGenerator.incrementAndGet())
                    .battleType(1)
                    .victory(true)
                    .resultType(1)
                    .duration(5000)
                    .totalRounds(3)
                    .stars(3)
                    .expGain(100L * difficulty)
                    .goldGain(50L * difficulty)
                    .drops(generateRandomDrops(dungeonId))
                    .build();
            results.add(result);
        }
        return Result.success(results);
    }

    @Override
    public Result<BattleDTO> startPvpBattle(long roleId, int battleType) {
        // 检查玩家是否已在战斗中
        if (playerBattleMap.containsKey(roleId)) {
            return Result.fail(ErrorCode.PARAM_ERROR, "已在战斗中");
        }

        // PVP 需要匹配系统，这里简化处理
        return Result.fail(ErrorCode.PARAM_ERROR, "请使用匹配功能");
    }

    @Override
    public Result<MatchResultDTO> matchPvp(long roleId, int battleType) {
        // 匹配逻辑 (简化实现)
        MatchResultDTO result = MatchResultDTO.builder()
                .status(0) // 匹配中
                .estimatedWaitTime(30)
                .waitedTime(0)
                .build();
        return Result.success(result);
    }

    @Override
    public Result<Void> cancelMatch(long roleId) {
        // 取消匹配逻辑
        return Result.success();
    }

    @Override
    public Result<Void> submitAction(long battleId, long roleId, BattleActionDTO action) {
        BattleContext context = battleEngine.getBattle(battleId);
        if (context == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "战斗不存在");
        }

        if (!context.getParticipantIds().contains(roleId)) {
            return Result.fail(ErrorCode.PARAM_ERROR, "不在此战斗中");
        }

        // 执行操作
        List<BattleAction> actions = battleEngine.executeAction(context, action);

        // 检查战斗是否结束
        if (context.getStatus() == BattleContext.BattleStatus.FINISHED) {
            onBattleEnd(context);
        }

        return Result.success();
    }

    @Override
    public Result<BattleDTO> getBattleInfo(long battleId) {
        BattleContext context = battleEngine.getBattle(battleId);
        if (context == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "战斗不存在");
        }
        return Result.success(toBattleDTO(context));
    }

    @Override
    public Result<BattleResultDTO> getBattleResult(long battleId) {
        BattleContext context = battleEngine.getBattle(battleId);
        if (context == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "战斗不存在");
        }
        if (context.getStatus() != BattleContext.BattleStatus.FINISHED) {
            return Result.fail(ErrorCode.PARAM_ERROR, "战斗未结束");
        }
        return Result.success(toBattleResultDTO(context));
    }

    @Override
    public Result<Void> exitBattle(long battleId, long roleId) {
        BattleContext context = battleEngine.getBattle(battleId);
        if (context != null && context.getParticipantIds().contains(roleId)) {
            // 标记为投降
            context.end(context.getMyUnits().values().stream()
                    .anyMatch(u -> u.getEntityId() == roleId) ? 2 : 1);
            onBattleEnd(context);
        }
        playerBattleMap.remove(roleId);
        return Result.success();
    }

    @Override
    public Result<List<BattleRecordDTO>> getBattleRecords(long roleId, int type, int page, int size) {
        // 从数据库查询战斗记录 (简化实现)
        return Result.success(Collections.emptyList());
    }

    // ==================== 私有方法 ====================

    /**
     * 初始化玩家单位
     */
    private void initPlayerUnits(BattleContext context, long roleId) {
        // 实际项目中应从玩家数据加载阵容
        BattleUnit unit = new BattleUnit();
        unit.setUnitId(1);
        unit.setEntityId(roleId);
        unit.setName("玩家");
        unit.setUnitType(1);
        unit.setPosition(1);
        unit.setHp(10000);
        unit.setMaxHp(10000);
        unit.setEnergy(0);
        unit.setMaxEnergy(100);
        unit.setAttack(1000);
        unit.setDefense(500);
        unit.setSpeed(100);
        unit.setCritRate(1500); // 15%
        unit.setCritDamage(15000); // 150%
        unit.setHitRate(9500); // 95%
        unit.setDodgeRate(500); // 5%

        // 添加普攻技能
        Skill normalAttack = new Skill();
        normalAttack.setSkillId(1);
        normalAttack.setName("普通攻击");
        normalAttack.setSkillType(0);
        normalAttack.setTargetType(1);
        normalAttack.setTargetCamp(1);
        normalAttack.setMaxCooldown(0);
        normalAttack.setEnergyCost(0);
        normalAttack.setEffects(List.of());
        unit.getSkills().add(normalAttack);

        context.addMyUnit(unit);
    }

    /**
     * 初始化副本敌人
     */
    private void initDungeonEnemies(BattleContext context, int dungeonId, int difficulty) {
        // 实际项目中应根据副本配置加载敌人
        int enemyCount = 1 + difficulty;
        for (int i = 0; i < enemyCount; i++) {
            BattleUnit enemy = new BattleUnit();
            enemy.setUnitId(100 + i);
            enemy.setEntityId(dungeonId * 1000L + i);
            enemy.setName("怪物" + (i + 1));
            enemy.setUnitType(2);
            enemy.setPosition(i + 1);
            enemy.setHp(5000L * difficulty);
            enemy.setMaxHp(5000L * difficulty);
            enemy.setEnergy(0);
            enemy.setMaxEnergy(100);
            enemy.setAttack(500L * difficulty);
            enemy.setDefense(300L * difficulty);
            enemy.setSpeed(80 + i * 10);
            enemy.setCritRate(1000);
            enemy.setCritDamage(15000);
            enemy.setHitRate(9000);
            enemy.setDodgeRate(300);

            // 添加普攻技能
            Skill normalAttack = new Skill();
            normalAttack.setSkillId(1);
            normalAttack.setName("普通攻击");
            normalAttack.setSkillType(0);
            normalAttack.setTargetType(1);
            normalAttack.setTargetCamp(1);
            normalAttack.setMaxCooldown(0);
            normalAttack.setEnergyCost(0);
            normalAttack.setEffects(List.of());
            enemy.getSkills().add(normalAttack);

            context.addEnemyUnit(enemy);
        }
    }

    /**
     * 战斗结束处理
     */
    private void onBattleEnd(BattleContext context) {
        // 移除玩家战斗映射
        context.getParticipantIds().forEach(playerBattleMap::remove);

        // 发送战斗结算消息
        sendBattleMessage(context, BattleMqMessage.BattleMessageType.BATTLE_SETTLE);

        // 延迟清理战斗
        // 实际项目中可以用定时任务清理
        battleEngine.removeBattle(context.getBattleId());

        log.info("战斗结束处理: battleId={}, winner={}", context.getBattleId(), context.getWinner());
    }

    /**
     * 发送战斗消息到 MQ
     */
    private void sendBattleMessage(BattleContext context, BattleMqMessage.BattleMessageType type) {
        BattleMqMessage message = new BattleMqMessage();
        message.setBattleId(context.getBattleId());
        message.setBattleType(context.getBattleType());
        message.setType(type);
        message.setParticipantIds(new ArrayList<>(context.getParticipantIds()));
        message.setDungeonId(context.getDungeonId());
        message.setDifficulty(context.getDifficulty());
        message.setDuration(context.getEndTime() - context.getStartTime());

        if (type == BattleMqMessage.BattleMessageType.BATTLE_SETTLE) {
            message.setWinnerIds(context.getWinner() == 1 
                    ? new ArrayList<>(context.getParticipantIds()) 
                    : Collections.emptyList());
        }

        mqProducer.send(MqExchange.BATTLE_RESULT, "battle.result." + context.getBattleType(), message);
    }

    /**
     * 生成随机掉落
     */
    private List<BattleResultDTO.RewardItem> generateRandomDrops(int dungeonId) {
        List<BattleResultDTO.RewardItem> drops = new ArrayList<>();
        Random random = new Random();
        int dropCount = 1 + random.nextInt(3);
        for (int i = 0; i < dropCount; i++) {
            drops.add(BattleResultDTO.RewardItem.builder()
                    .itemId(1001 + random.nextInt(10))
                    .itemName("道具" + (i + 1))
                    .count(1 + random.nextInt(5))
                    .quality(1 + random.nextInt(3))
                    .build());
        }
        return drops;
    }

    /**
     * 转换为 DTO
     */
    private BattleDTO toBattleDTO(BattleContext context) {
        return BattleDTO.builder()
                .battleId(context.getBattleId())
                .battleType(context.getBattleType())
                .status(context.getStatus().ordinal())
                .currentRound(context.getCurrentRound())
                .maxRound(context.getMaxRound())
                .dungeonId(context.getDungeonId())
                .difficulty(context.getDifficulty())
                .myUnits(context.getMyUnits().values().stream().map(this::toUnitDTO).toList())
                .enemyUnits(context.getEnemyUnits().values().stream().map(this::toUnitDTO).toList())
                .startTime(context.getStartTime())
                .timeout(battleTimeoutSeconds)
                .randomSeed(context.getRandomSeed())
                .build();
    }

    /**
     * 转换单位为 DTO
     */
    private BattleUnitDTO toUnitDTO(BattleUnit unit) {
        return BattleUnitDTO.builder()
                .unitId(unit.getUnitId())
                .entityId(unit.getEntityId())
                .name(unit.getName())
                .unitType(unit.getUnitType())
                .camp(unit.getCamp())
                .position(unit.getPosition())
                .hp(unit.getHp())
                .maxHp(unit.getMaxHp())
                .energy(unit.getEnergy())
                .maxEnergy(unit.getMaxEnergy())
                .attack(unit.getAttack())
                .defense(unit.getDefense())
                .speed(unit.getSpeed())
                .critRate(unit.getCritRate())
                .critDamage(unit.getCritDamage())
                .alive(unit.isAlive())
                .skills(unit.getSkills().stream().map(this::toSkillDTO).toList())
                .buffs(unit.getBuffs().stream().map(this::toBuffDTO).toList())
                .build();
    }

    /**
     * 转换技能为 DTO
     */
    private BattleUnitDTO.SkillDTO toSkillDTO(Skill skill) {
        return BattleUnitDTO.SkillDTO.builder()
                .skillId(skill.getSkillId())
                .skillName(skill.getName())
                .skillType(skill.getSkillType())
                .cooldown(skill.getCurrentCooldown())
                .maxCooldown(skill.getMaxCooldown())
                .energyCost(skill.getEnergyCost())
                .available(skill.isAvailable())
                .build();
    }

    /**
     * 转换 Buff 为 DTO
     */
    private BattleUnitDTO.BuffDTO toBuffDTO(Buff buff) {
        return BattleUnitDTO.BuffDTO.builder()
                .buffId(buff.getBuffId())
                .buffName(buff.getName())
                .buffType(buff.getBuffType())
                .stackCount(buff.getStackCount())
                .duration(buff.getDuration())
                .value(buff.getValue())
                .build();
    }

    /**
     * 转换为结果 DTO
     */
    private BattleResultDTO toBattleResultDTO(BattleContext context) {
        return BattleResultDTO.builder()
                .battleId(context.getBattleId())
                .battleType(context.getBattleType())
                .victory(context.getWinner() == 1)
                .resultType(1)
                .duration(context.getEndTime() - context.getStartTime())
                .totalRounds(context.getCurrentRound())
                .stars(context.getWinner() == 1 ? 3 : 0)
                .expGain(context.getWinner() == 1 ? 100L : 0)
                .goldGain(context.getWinner() == 1 ? 50L : 0)
                .build();
    }
}
