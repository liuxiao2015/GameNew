package com.game.service.battle.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗上下文
 * <p>
 * 维护一场战斗的完整状态
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Data
public class BattleContext {

    /**
     * 战斗ID
     */
    private long battleId;

    /**
     * 战斗类型 (1:PVE 2:PVP竞技 3:PVP匹配)
     */
    private int battleType;

    /**
     * 战斗状态
     */
    private BattleStatus status;

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
     * 我方单位 (unitId -> BattleUnit)
     */
    private Map<Integer, BattleUnit> myUnits = new ConcurrentHashMap<>();

    /**
     * 敌方单位 (unitId -> BattleUnit)
     */
    private Map<Integer, BattleUnit> enemyUnits = new ConcurrentHashMap<>();

    /**
     * 所有单位 (unitId -> BattleUnit)
     */
    private Map<Integer, BattleUnit> allUnits = new ConcurrentHashMap<>();

    /**
     * 行动顺序队列
     */
    private List<Integer> actionOrder = new ArrayList<>();

    /**
     * 当前行动单位索引
     */
    private int currentActionIndex;

    /**
     * 战斗开始时间
     */
    private long startTime;

    /**
     * 战斗结束时间
     */
    private long endTime;

    /**
     * 超时时间 (毫秒)
     */
    private long timeoutMs;

    /**
     * 随机种子
     */
    private long randomSeed;

    /**
     * 随机数生成器
     */
    private Random random;

    /**
     * 战斗操作序列号
     */
    private int actionSequence;

    /**
     * 战斗记录 (用于回放)
     */
    private List<BattleAction> battleLog = new ArrayList<>();

    /**
     * 参与者角色ID
     */
    private Set<Long> participantIds = new HashSet<>();

    /**
     * 胜利方 (1:我方 2:敌方 0:未决)
     */
    private int winner;

    /**
     * 初始化战斗
     */
    public void init() {
        this.status = BattleStatus.PREPARING;
        this.currentRound = 0;
        this.startTime = System.currentTimeMillis();
        this.randomSeed = System.nanoTime();
        this.random = new Random(randomSeed);
        this.actionSequence = 0;
        this.winner = 0;
    }

    /**
     * 开始战斗
     */
    public void start() {
        this.status = BattleStatus.IN_PROGRESS;
        this.currentRound = 1;
        calculateActionOrder();
        log.info("战斗开始: battleId={}, type={}, myUnits={}, enemyUnits={}", 
                battleId, battleType, myUnits.size(), enemyUnits.size());
    }

    /**
     * 计算行动顺序 (按速度排序)
     */
    public void calculateActionOrder() {
        actionOrder.clear();
        List<BattleUnit> aliveUnits = new ArrayList<>();
        
        myUnits.values().stream().filter(BattleUnit::isAlive).forEach(aliveUnits::add);
        enemyUnits.values().stream().filter(BattleUnit::isAlive).forEach(aliveUnits::add);
        
        // 按速度降序排列，速度相同则随机
        aliveUnits.sort((a, b) -> {
            if (a.getSpeed() != b.getSpeed()) {
                return b.getSpeed() - a.getSpeed();
            }
            return random.nextBoolean() ? 1 : -1;
        });
        
        aliveUnits.forEach(unit -> actionOrder.add(unit.getUnitId()));
        currentActionIndex = 0;
    }

    /**
     * 获取当前行动单位
     */
    public BattleUnit getCurrentActionUnit() {
        if (actionOrder.isEmpty() || currentActionIndex >= actionOrder.size()) {
            return null;
        }
        return allUnits.get(actionOrder.get(currentActionIndex));
    }

    /**
     * 下一个行动单位
     */
    public BattleUnit nextActionUnit() {
        currentActionIndex++;
        
        // 如果当前回合所有单位都行动完毕，进入下一回合
        if (currentActionIndex >= actionOrder.size()) {
            nextRound();
        }
        
        return getCurrentActionUnit();
    }

    /**
     * 进入下一回合
     */
    public void nextRound() {
        currentRound++;
        currentActionIndex = 0;
        
        if (currentRound > maxRound) {
            // 超过最大回合，战斗结束
            end(determineWinnerByHp());
            return;
        }
        
        // 回合开始时的处理
        processRoundStart();
        
        // 重新计算行动顺序
        calculateActionOrder();
        
        log.debug("战斗进入第 {} 回合: battleId={}", currentRound, battleId);
    }

    /**
     * 回合开始处理
     */
    private void processRoundStart() {
        // 处理所有单位的回合开始效果
        allUnits.values().forEach(unit -> {
            if (unit.isAlive()) {
                unit.onRoundStart();
            }
        });
    }

    /**
     * 根据血量比例判定胜负
     */
    private int determineWinnerByHp() {
        long myTotalHp = myUnits.values().stream()
                .filter(BattleUnit::isAlive)
                .mapToLong(BattleUnit::getHp)
                .sum();
        long myMaxHp = myUnits.values().stream()
                .mapToLong(BattleUnit::getMaxHp)
                .sum();
        
        long enemyTotalHp = enemyUnits.values().stream()
                .filter(BattleUnit::isAlive)
                .mapToLong(BattleUnit::getHp)
                .sum();
        long enemyMaxHp = enemyUnits.values().stream()
                .mapToLong(BattleUnit::getMaxHp)
                .sum();
        
        double myHpRatio = myMaxHp > 0 ? (double) myTotalHp / myMaxHp : 0;
        double enemyHpRatio = enemyMaxHp > 0 ? (double) enemyTotalHp / enemyMaxHp : 0;
        
        return myHpRatio >= enemyHpRatio ? 1 : 2;
    }

    /**
     * 结束战斗
     */
    public void end(int winner) {
        this.status = BattleStatus.FINISHED;
        this.winner = winner;
        this.endTime = System.currentTimeMillis();
        log.info("战斗结束: battleId={}, winner={}, duration={}ms", 
                battleId, winner, endTime - startTime);
    }

    /**
     * 添加我方单位
     */
    public void addMyUnit(BattleUnit unit) {
        unit.setCamp(1);
        myUnits.put(unit.getUnitId(), unit);
        allUnits.put(unit.getUnitId(), unit);
    }

    /**
     * 添加敌方单位
     */
    public void addEnemyUnit(BattleUnit unit) {
        unit.setCamp(2);
        enemyUnits.put(unit.getUnitId(), unit);
        allUnits.put(unit.getUnitId(), unit);
    }

    /**
     * 检查战斗是否结束
     */
    public boolean checkBattleEnd() {
        boolean myAllDead = myUnits.values().stream().noneMatch(BattleUnit::isAlive);
        boolean enemyAllDead = enemyUnits.values().stream().noneMatch(BattleUnit::isAlive);
        
        if (myAllDead) {
            end(2); // 敌方胜利
            return true;
        }
        if (enemyAllDead) {
            end(1); // 我方胜利
            return true;
        }
        
        // 检查超时
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            end(determineWinnerByHp());
            return true;
        }
        
        return false;
    }

    /**
     * 记录战斗操作
     */
    public void recordAction(BattleAction action) {
        action.setSequence(++actionSequence);
        action.setRound(currentRound);
        action.setTimestamp(System.currentTimeMillis());
        battleLog.add(action);
    }

    /**
     * 获取随机数 [0, bound)
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    /**
     * 获取随机数 [min, max]
     */
    public int nextInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * 战斗状态
     */
    public enum BattleStatus {
        PREPARING,   // 准备中
        IN_PROGRESS, // 进行中
        PAUSED,      // 暂停
        FINISHED     // 已结束
    }
}
