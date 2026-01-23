package com.game.service.game.actor;

import com.game.actor.core.BaseActor;
import com.game.actor.core.MessageHandler;
import com.game.config.container.LevelConfigContainer;
import com.game.config.config.LevelConfig;
import com.game.core.event.EventBus;
import com.game.entity.player.PlayerData;
import com.game.service.game.event.PlayerEvents;
import com.game.service.game.repository.PlayerRepository;
import com.game.service.game.service.BagService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 Actor
 * <p>
 * 封装玩家所有数据和业务逻辑，保证线程安全
 * 所有对玩家的操作都在 Actor 线程内执行
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - BaseActor: 基于虚拟线程的 Actor 模型
 * - MessageHandler: 声明式消息处理
 * - EventBus: 事件发布（升级、货币变化等）
 * - ConfigContainer: 配置数据读取
 * </pre>
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
     * 事件总线 (用于发布玩家事件)
     */
    @Setter
    private EventBus eventBus;

    /**
     * 等级配置容器
     */
    @Setter
    private LevelConfigContainer levelConfigContainer;

    /**
     * 玩家 Session ID
     */
    @Getter
    private String sessionId;

    /**
     * 服务器 ID
     */
    @Getter
    private int serverId;

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
        this.serverId = loginData.serverId();
        
        PlayerData data = getData();
        data.setLastLoginTime(System.currentTimeMillis());
        markDirty();

        log.info("玩家登录: roleId={}, roleName={}, level={}",
                getActorId(), data.getRoleName(), data.getLevel());

        // 发布登录事件
        if (eventBus != null) {
            eventBus.publish(new PlayerEvents.PlayerLoginEvent(
                    getActorId(), data.getRoleName(), data.getLevel(), sessionId, serverId));
        }
    }

    /**
     * 玩家登出
     */
    @MessageHandler("LOGOUT")
    public void onLogout() {
        PlayerData data = getData();
        long now = System.currentTimeMillis();
        
        // 计算在线时长
        long onlineTime = 0;
        if (data.getLastLoginTime() > 0) {
            onlineTime = (now - data.getLastLoginTime()) / 1000;
            data.setTotalOnlineTime(data.getTotalOnlineTime() + onlineTime);
        }
        
        data.setLastLogoutTime(now);
        markDirty();

        // 立即保存
        saveData();

        log.info("玩家登出: roleId={}, roleName={}", getActorId(), data.getRoleName());

        // 发布登出事件
        if (eventBus != null) {
            eventBus.publish(new PlayerEvents.PlayerLogoutEvent(
                    getActorId(), data.getRoleName(), onlineTime));
        }
        
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

        // 发布金币变化事件
        if (eventBus != null) {
            eventBus.publish(new PlayerEvents.GoldChangeEvent(
                    getActorId(), before, after, param.reason()));
        }

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

        // 发布钻石变化事件
        if (eventBus != null) {
            eventBus.publish(new PlayerEvents.DiamondChangeEvent(
                    getActorId(), before, after, param.reason()));
        }

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

        // 检查升级
        int newLevel = calculateLevel(newExp);
        if (newLevel > data.getLevel()) {
            int oldLevel = data.getLevel();
            data.setLevel(newLevel);

            log.info("玩家升级: roleId={}, {} -> {}", getActorId(), oldLevel, newLevel);

            // 发布升级事件
            if (eventBus != null) {
                eventBus.publish(new PlayerEvents.PlayerLevelUpEvent(
                        getActorId(), oldLevel, newLevel));
            }
        }

        markDirty();
    }

    /**
     * 计算等级
     */
    private int calculateLevel(long exp) {
        // 优先使用配置
        if (levelConfigContainer != null) {
            return levelConfigContainer.calculateLevel(exp);
        }
        
        // 兜底逻辑
        int level = 1;
        long[] expTable = {0, 100, 300, 650, 1200, 2000, 3100, 4550, 6400, 8700};
        for (int i = 0; i < expTable.length; i++) {
            if (exp >= expTable[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    // ==================== 物品操作 ====================

    /**
     * 添加物品
     */
    @MessageHandler("ADD_ITEM")
    public void onAddItem(AddItemData param) {
        PlayerData data = getData();
        long currentCount = data.getBagItems().getOrDefault(param.itemId(), 0L);
        data.getBagItems().put(param.itemId(), currentCount + param.count());
        markDirty();

        log.debug("添加物品: roleId={}, itemId={}, count={}, reason={}",
                getActorId(), param.itemId(), param.count(), param.reason());
    }

    /**
     * 移除物品
     */
    @MessageHandler("REMOVE_ITEM")
    public void onRemoveItem(RemoveItemData param) {
        PlayerData data = getData();
        long currentCount = data.getBagItems().getOrDefault(param.itemId(), 0L);
        long newCount = currentCount - param.count();
        
        if (newCount <= 0) {
            data.getBagItems().remove(param.itemId());
        } else {
            data.getBagItems().put(param.itemId(), newCount);
        }
        markDirty();

        log.debug("移除物品: roleId={}, itemId={}, count={}, reason={}",
                getActorId(), param.itemId(), param.count(), param.reason());
    }

    // ==================== 信息更新 ====================

    /**
     * 更新玩家信息
     */
    @MessageHandler("UPDATE_INFO")
    public void onUpdateInfo(UpdateInfoData param) {
        PlayerData data = getData();
        if (param.avatarId() > 0) {
            data.setAvatarId(param.avatarId());
        }
        if (param.frameId() > 0) {
            data.setFrameId(param.frameId());
        }
        if (param.signature() != null) {
            data.setSignature(param.signature());
        }
        markDirty();
    }

    /**
     * 修改名字
     */
    @MessageHandler("CHANGE_NAME")
    public void onChangeName(String newName) {
        PlayerData data = getData();
        data.setRoleName(newName);
        markDirty();
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

    public record LoginData(String sessionId, int serverId) {}
    public record AddGoldData(long amount, String reason) {}
    public record AddGoldResult(boolean success, long gold, String error) {}
    public record AddDiamondData(long amount, String reason) {}
    public record AddDiamondResult(boolean success, long diamond, String error) {}
    public record AddExpData(long amount, String reason) {}
    public record CostEnergyData(int amount, String reason) {}
    public record CostEnergyResult(boolean success, int energy, String error) {}
    public record SetGuildData(long guildId, String guildName, int position) {}
    public record AddItemData(int itemId, long count, String reason) {}
    public record RemoveItemData(int itemId, long count, String reason) {}
    public record UpdateInfoData(int avatarId, int frameId, String signature) {}
}
