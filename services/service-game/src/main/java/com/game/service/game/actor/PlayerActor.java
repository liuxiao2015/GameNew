package com.game.service.game.actor;

import com.game.actor.core.Actor;
import com.game.actor.core.ActorMessage;
import com.game.common.constants.GameConstants;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.game.entity.player.PlayerData;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家 Actor
 * <p>
 * 负责管理单个玩家的数据和业务逻辑，实现无锁化处理
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class PlayerActor extends Actor<PlayerData> {

    /**
     * Actor 类型
     */
    public static final String ACTOR_TYPE = "PLAYER";

    private final RedisService redisService;
    private final MongoService mongoService;

    public PlayerActor(long roleId, RedisService redisService, MongoService mongoService) {
        super(roleId, ACTOR_TYPE, GameConstants.ACTOR_MAILBOX_MAX_SIZE);
        this.redisService = redisService;
        this.mongoService = mongoService;
    }

    @Override
    protected PlayerData loadData() {
        long roleId = getActorId();

        // 1. 先从 Redis 缓存加载
        String cacheKey = GameConstants.REDIS_PLAYER_PREFIX + roleId;
        PlayerData data = redisService.getObject(cacheKey, PlayerData.class);

        if (data != null) {
            log.debug("从 Redis 加载玩家数据: roleId={}", roleId);
            return data;
        }

        // 2. 从 MongoDB 加载
        data = mongoService.findById(roleId, PlayerData.class);

        if (data != null) {
            log.debug("从 MongoDB 加载玩家数据: roleId={}", roleId);
            // 回写 Redis 缓存
            redisService.setObject(cacheKey, data, Duration.ofHours(24));
            return data;
        }

        log.warn("玩家数据不存在: roleId={}", roleId);
        return null;
    }

    @Override
    protected void saveData() {
        if (data == null) {
            return;
        }

        long roleId = getActorId();
        String cacheKey = GameConstants.REDIS_PLAYER_PREFIX + roleId;

        try {
            // 1. 保存到 Redis
            redisService.setObject(cacheKey, data, Duration.ofHours(24));

            // 2. 保存到 MongoDB
            mongoService.save(data);

            log.debug("保存玩家数据成功: roleId={}", roleId);

        } catch (Exception e) {
            log.error("保存玩家数据失败: roleId={}", roleId, e);
            throw e;
        }
    }

    @Override
    protected void handleMessage(ActorMessage message) {
        Object msgData = message.getData();

        // 处理内部服务消息 (使用 instanceof pattern matching)
        if (msgData instanceof SetGuildMessage msg) {
            handleSetGuild(msg);
        } else if (msgData instanceof DeductCurrencyMessage msg) {
            handleDeductCurrency(msg);
        } else if (msgData instanceof CheckCurrencyMessage msg) {
            handleCheckCurrency(msg);
        } else if (msgData instanceof GetCombatPowerMessage msg) {
            handleGetCombatPower(msg);
        } else if (msgData instanceof GetDataMessage msg) {
            handleGetData(msg);
        } else if (msgData instanceof DailyResetMessage msg) {
            handleDailyReset(msg);
        } else if (msgData instanceof WeeklyResetMessage msg) {
            handleWeeklyReset(msg);
        } else if (msgData instanceof SendMailMessage msg) {
            handleSendMail(msg);
        }
        // 处理标准消息类型
        else {
            String type = message.getType();
            switch (type) {
                case ActorMessage.TYPE_SAVE -> saveData();
                case ActorMessage.TYPE_STOP -> stop();
                case MessageTypes.ADD_EXP -> handleAddExp(message);
                case MessageTypes.ADD_GOLD -> handleAddGold(message);
                case MessageTypes.ADD_DIAMOND -> handleAddDiamond(message);
                case MessageTypes.ADD_ITEM -> handleAddItem(message);
                case MessageTypes.LEVEL_UP -> handleLevelUp(message);
                case MessageTypes.CHANGE_NAME -> handleChangeName(message);
                case MessageTypes.CHANGE_AVATAR -> handleChangeAvatar(message);
                case MessageTypes.JOIN_GUILD -> handleJoinGuild(message);
                case MessageTypes.LEAVE_GUILD -> handleLeaveGuild(message);
                default -> log.warn("未知的消息类型: actorId={}, type={}", getActorId(), type);
            }
        }
    }

    @Override
    protected void onStop() {
        // 从在线玩家集合中移除
        redisService.sRemove(GameConstants.REDIS_ONLINE_SET, String.valueOf(getActorId()));
    }

    // ==================== 内部服务消息处理 ====================

    private void handleSetGuild(SetGuildMessage msg) {
        data.setGuildId(msg.guildId);
        data.setGuildName(msg.guildName);
        // 注：PlayerData 可能需要添加 guildPosition 字段
        markDirty();

        log.info("设置公会信息: roleId={}, guildId={}", getActorId(), msg.guildId);
        msg.future.complete(Result.success());
    }

    private void handleDeductCurrency(DeductCurrencyMessage msg) {
        boolean success = false;

        if (msg.currencyType == 1) { // 金币
            if (data.getGold() >= msg.amount) {
                data.setGold(data.getGold() - msg.amount);
                success = true;
            }
        } else if (msg.currencyType == 2) { // 钻石
            if (data.getDiamond() >= msg.amount) {
                data.setDiamond(data.getDiamond() - msg.amount);
                success = true;
            }
        }

        if (success) {
            markDirty();
            log.info("扣除货币: roleId={}, type={}, amount={}, reason={}",
                    getActorId(), msg.currencyType, msg.amount, msg.reason);
            msg.future.complete(Result.success());
        } else {
            msg.future.complete(Result.fail(ErrorCode.CURRENCY_NOT_ENOUGH));
        }
    }

    private void handleCheckCurrency(CheckCurrencyMessage msg) {
        boolean enough = false;

        if (msg.currencyType == 1) { // 金币
            enough = data.getGold() >= msg.amount;
        } else if (msg.currencyType == 2) { // 钻石
            enough = data.getDiamond() >= msg.amount;
        }

        msg.future.complete(Result.success(enough));
    }

    private void handleGetCombatPower(GetCombatPowerMessage msg) {
        msg.future.complete(Result.success(data.getCombatPower()));
    }

    private void handleGetData(GetDataMessage msg) {
        msg.future.complete(Result.success(data));
    }

    private void handleDailyReset(DailyResetMessage msg) {
        String today = LocalDate.now().toString();
        PlayerData.DailyData dailyData = data.getDailyData();

        if (dailyData == null) {
            dailyData = new PlayerData.DailyData();
            data.setDailyData(dailyData);
        }

        // 检查是否已经重置过
        if (today.equals(dailyData.getDate())) {
            msg.future.complete(Result.success());
            return;
        }

        // 执行每日重置
        dailyData.setDate(today);
        dailyData.setSigned(false);
        dailyData.setArenaCount(0);
        dailyData.getDungeonCounts().clear();
        dailyData.getBuyCounts().clear();

        // 恢复体力
        data.setEnergy(data.getMaxEnergy());

        markDirty();
        log.info("玩家每日重置完成: roleId={}", getActorId());
        msg.future.complete(Result.success());
    }

    private void handleWeeklyReset(WeeklyResetMessage msg) {
        log.info("玩家每周重置完成: roleId={}", getActorId());
        markDirty();
        msg.future.complete(Result.success());
    }

    private void handleSendMail(SendMailMessage msg) {
        // TODO: 实现邮件系统
        log.info("发送邮件: roleId={}, title={}", getActorId(), msg.title);
        msg.future.complete(Result.success());
    }

    // ==================== 标准消息处理 ====================

    private void handleAddExp(ActorMessage message) {
        AddExpRequest req = message.getData();
        if (req == null) return;

        long oldExp = data.getExp();
        data.setExp(oldExp + req.exp());
        markDirty();

        log.debug("增加经验: roleId={}, addExp={}, totalExp={}", getActorId(), req.exp(), data.getExp());
        checkLevelUp();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(data.getExp(), null);
        }
    }

    private void handleAddGold(ActorMessage message) {
        AddGoldRequest req = message.getData();
        if (req == null) return;

        data.setGold(data.getGold() + req.gold());
        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(data.getGold(), null);
        }
    }

    private void handleAddDiamond(ActorMessage message) {
        AddDiamondRequest req = message.getData();
        if (req == null) return;

        data.setDiamond(data.getDiamond() + req.diamond());
        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(data.getDiamond(), null);
        }
    }

    private void handleAddItem(ActorMessage message) {
        AddItemRequest req = message.getData();
        if (req == null) return;

        Map<Integer, Long> bagItems = data.getBagItems();
        long currentCount = bagItems.getOrDefault(req.itemId(), 0L);
        bagItems.put(req.itemId(), currentCount + req.count());
        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void handleLevelUp(ActorMessage message) {
        int newLevel = data.getLevel() + 1;
        data.setLevel(newLevel);
        markDirty();

        log.info("玩家升级: roleId={}, newLevel={}", getActorId(), newLevel);

        if (message.getCallback() != null) {
            message.getCallback().onComplete(newLevel, null);
        }
    }

    private void handleChangeName(ActorMessage message) {
        String newName = message.getData();
        if (newName == null || newName.isBlank()) return;

        data.setRoleName(newName);
        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(newName, null);
        }
    }

    private void handleChangeAvatar(ActorMessage message) {
        Integer avatarId = message.getData();
        if (avatarId == null) return;

        data.setAvatarId(avatarId);
        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(avatarId, null);
        }
    }

    private void handleJoinGuild(ActorMessage message) {
        JoinGuildRequest req = message.getData();
        if (req == null) return;

        data.setGuildId(req.guildId());
        data.setGuildName(req.guildName());
        markDirty();

        log.info("加入公会: roleId={}, guildId={}", getActorId(), req.guildId());

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void handleLeaveGuild(ActorMessage message) {
        data.setGuildId(0);
        data.setGuildName(null);
        markDirty();

        log.info("退出公会: roleId={}", getActorId());

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void checkLevelUp() {
        // TODO: 根据经验表计算是否升级
    }

    // ==================== 消息类型定义 ====================

    /** 设置公会信息 */
    public record SetGuildMessage(long guildId, String guildName, int position,
                                  CompletableFuture<Result<Void>> future) {}

    /** 扣除货币 */
    public record DeductCurrencyMessage(int currencyType, long amount, String reason,
                                        CompletableFuture<Result<Void>> future) {}

    /** 检查货币 */
    public record CheckCurrencyMessage(int currencyType, long amount,
                                       CompletableFuture<Result<Boolean>> future) {}

    /** 获取战力 */
    public record GetCombatPowerMessage(CompletableFuture<Result<Long>> future) {}

    /** 获取玩家数据 */
    public record GetDataMessage(CompletableFuture<Result<PlayerData>> future) {}

    /** 每日重置 */
    public record DailyResetMessage(CompletableFuture<Result<Void>> future) {}

    /** 每周重置 */
    public record WeeklyResetMessage(CompletableFuture<Result<Void>> future) {}

    /** 发送邮件 */
    public record SendMailMessage(String title, String content, Map<Integer, Long> attachments,
                                  CompletableFuture<Result<Void>> future) {}

    /** 增加经验 */
    public record AddExpRequest(long exp, String reason) {}

    /** 增加金币 */
    public record AddGoldRequest(long gold, String reason) {}

    /** 增加钻石 */
    public record AddDiamondRequest(long diamond, String reason) {}

    /** 增加物品 */
    public record AddItemRequest(int itemId, long count, String reason) {}

    /** 加入公会 */
    public record JoinGuildRequest(long guildId, String guildName) {}

    /**
     * 消息类型常量
     */
    public static class MessageTypes {
        public static final String ADD_EXP = "ADD_EXP";
        public static final String ADD_GOLD = "ADD_GOLD";
        public static final String ADD_DIAMOND = "ADD_DIAMOND";
        public static final String ADD_ITEM = "ADD_ITEM";
        public static final String LEVEL_UP = "LEVEL_UP";
        public static final String CHANGE_NAME = "CHANGE_NAME";
        public static final String CHANGE_AVATAR = "CHANGE_AVATAR";
        public static final String JOIN_GUILD = "JOIN_GUILD";
        public static final String LEAVE_GUILD = "LEAVE_GUILD";
    }
}
