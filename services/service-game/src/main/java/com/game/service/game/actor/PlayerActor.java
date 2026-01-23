package com.game.service.game.actor;

import com.game.actor.core.BaseActor;
import com.game.actor.core.MessageHandler;
import com.game.entity.player.PlayerData;
import com.game.service.game.repository.PlayerRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 Actor
 * <p>
 * 封装玩家所有数据和业务逻辑，保证线程安全
 * 所有对玩家的操作都在 Actor 线程内执行
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class PlayerActor extends BaseActor<PlayerData> {

    /**
     * 玩家数据仓库
     */
    private final PlayerRepository playerRepository;

    /**
     * 玩家 Session ID
     */
    @Getter
    private String sessionId;

    public PlayerActor(long roleId, PlayerRepository playerRepository) {
        super(roleId, "Player", 1000);
        this.playerRepository = playerRepository;
    }

    @Override
    protected PlayerData loadData() {
        return playerRepository.findById(getActorId()).orElse(null);
    }

    @Override
    protected void saveData() {
        if (data != null) {
            playerRepository.save(data);
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家登录
     */
    @MessageHandler("LOGIN")
    public void onLogin(LoginData loginData) {
        this.sessionId = loginData.sessionId();
        
        PlayerData data = getData();
        data.setLastLoginTime(System.currentTimeMillis());
        markDirty();

        log.info("玩家登录: roleId={}, roleName={}, level={}",
                getActorId(), data.getRoleName(), data.getLevel());
    }

    /**
     * 玩家登出
     */
    @MessageHandler("LOGOUT")
    public void onLogout() {
        PlayerData data = getData();
        long now = System.currentTimeMillis();
        
        // 计算在线时长
        if (data.getLastLoginTime() > 0) {
            long onlineTime = (now - data.getLastLoginTime()) / 1000;
            data.setTotalOnlineTime(data.getTotalOnlineTime() + onlineTime);
        }
        
        data.setLastLogoutTime(now);
        markDirty();

        // 立即保存
        saveData();

        log.info("玩家登出: roleId={}, roleName={}", getActorId(), data.getRoleName());
        
        this.sessionId = null;
    }

    // ==================== 货币操作 ====================

    /**
     * 增加金币
     */
    @MessageHandler("ADD_GOLD")
    public AddGoldResult onAddGold(AddGoldData param) {
        PlayerData data = getData();
        long before = data.getGold();
        long after = before + param.amount();
        
        if (after < 0) {
            return new AddGoldResult(false, before, "金币不足");
        }
        
        data.setGold(after);
        markDirty();

        log.debug("金币变化: roleId={}, before={}, change={}, after={}",
                getActorId(), before, param.amount(), after);

        return new AddGoldResult(true, after, null);
    }

    /**
     * 增加钻石
     */
    @MessageHandler("ADD_DIAMOND")
    public AddDiamondResult onAddDiamond(AddDiamondData param) {
        PlayerData data = getData();
        long before = data.getDiamond();
        long after = before + param.amount();
        
        if (after < 0) {
            return new AddDiamondResult(false, before, "钻石不足");
        }
        
        data.setDiamond(after);
        markDirty();

        return new AddDiamondResult(true, after, null);
    }

    // ==================== 经验与等级 ====================

    /**
     * 增加经验
     */
    @MessageHandler("ADD_EXP")
    public void onAddExp(AddExpData param) {
        PlayerData data = getData();
        long newExp = data.getExp() + param.amount();
        data.setExp(newExp);

        // 检查升级 (简化逻辑, 实际应查询等级配置)
        int newLevel = calculateLevel(newExp);
        if (newLevel > data.getLevel()) {
            int oldLevel = data.getLevel();
            data.setLevel(newLevel);

            log.info("玩家升级: roleId={}, {} -> {}", getActorId(), oldLevel, newLevel);
        }

        markDirty();
    }

    /**
     * 计算等级 (简化版本)
     */
    private int calculateLevel(long exp) {
        // 实际应该注入 LevelConfigContainer 查询
        int level = 1;
        long[] expTable = {0, 100, 300, 650, 1200, 2000, 3100, 4550, 6400, 8700};
        for (int i = 0; i < expTable.length; i++) {
            if (exp >= expTable[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    // ==================== 体力操作 ====================

    /**
     * 消耗体力
     */
    @MessageHandler("COST_ENERGY")
    public CostEnergyResult onCostEnergy(CostEnergyData param) {
        PlayerData data = getData();
        
        // 先恢复体力
        recoverEnergy(data);
        
        if (data.getEnergy() < param.amount()) {
            return new CostEnergyResult(false, data.getEnergy(), "体力不足");
        }
        
        data.setEnergy(data.getEnergy() - param.amount());
        markDirty();

        return new CostEnergyResult(true, data.getEnergy(), null);
    }

    /**
     * 恢复体力
     */
    private void recoverEnergy(PlayerData data) {
        if (data.getEnergy() >= data.getMaxEnergy()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long recoverTime = data.getEnergyRecoverTime();
        if (recoverTime <= 0) {
            recoverTime = now;
            data.setEnergyRecoverTime(recoverTime);
        }

        // 每 6 分钟恢复 1 点
        long interval = 360_000L;
        long elapsed = now - recoverTime;
        int recovered = (int) (elapsed / interval);
        
        if (recovered > 0) {
            int newEnergy = Math.min(data.getEnergy() + recovered, data.getMaxEnergy());
            data.setEnergy(newEnergy);
            data.setEnergyRecoverTime(recoverTime + recovered * interval);
            markDirty();
        }
    }

    // ==================== 公会相关 ====================

    /**
     * 设置公会信息
     */
    @MessageHandler("SET_GUILD")
    public void onSetGuild(SetGuildData param) {
        PlayerData data = getData();
        data.setGuildId(param.guildId());
        data.setGuildName(param.guildName());
        data.setGuildPosition(param.position());
        markDirty();
    }

    // ==================== 消息数据类 ====================

    public record LoginData(String sessionId) {}
    public record AddGoldData(long amount, String reason) {}
    public record AddGoldResult(boolean success, long gold, String error) {}
    public record AddDiamondData(long amount, String reason) {}
    public record AddDiamondResult(boolean success, long diamond, String error) {}
    public record AddExpData(long amount, String reason) {}
    public record CostEnergyData(int amount, String reason) {}
    public record CostEnergyResult(boolean success, int energy, String error) {}
    public record SetGuildData(long guildId, String guildName, int position) {}
}
