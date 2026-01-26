package com.game.service.guild.actor;

import com.game.actor.core.Actor;
import com.game.actor.core.ActorMessage;
import com.game.common.constants.GameConstants;
import com.game.common.result.Result;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.game.entity.document.GuildData;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 公会 Actor
 * <p>
 * 负责管理公会数据和业务逻辑，实现无锁化处理
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class GuildActor extends Actor<GuildData> {

    /**
     * Actor 类型
     */
    public static final String ACTOR_TYPE = "GUILD";

    private final RedisService redisService;
    private final MongoService mongoService;

    public GuildActor(long guildId, RedisService redisService, MongoService mongoService) {
        super(guildId, ACTOR_TYPE, 5000);
        this.redisService = redisService;
        this.mongoService = mongoService;
    }

    @Override
    protected GuildData loadData() {
        long guildId = getActorId();

        // 1. 从 Redis 缓存加载
        String cacheKey = GameConstants.REDIS_GUILD_PREFIX + guildId;
        GuildData data = redisService.getObject(cacheKey, GuildData.class);

        if (data != null) {
            log.debug("从 Redis 加载公会数据: guildId={}", guildId);
            return data;
        }

        // 2. 从 MongoDB 加载
        data = mongoService.findById(guildId, GuildData.class);

        if (data != null) {
            log.debug("从 MongoDB 加载公会数据: guildId={}", guildId);
            redisService.setObject(cacheKey, data, Duration.ofHours(24));
            return data;
        }

        log.warn("公会数据不存在: guildId={}", guildId);
        return null;
    }

    @Override
    protected void saveData() {
        if (data == null) {
            return;
        }

        long guildId = getActorId();
        String cacheKey = GameConstants.REDIS_GUILD_PREFIX + guildId;

        try {
            redisService.setObject(cacheKey, data, Duration.ofHours(24));
            mongoService.save(data);
            log.debug("保存公会数据成功: guildId={}", guildId);
        } catch (Exception e) {
            log.error("保存公会数据失败: guildId={}", guildId, e);
            throw e;
        }
    }

    @Override
    protected void handleMessage(ActorMessage message) {
        Object msgData = message.getData();

        // 处理内部服务消息
        if (msgData instanceof MemberOnlineMessage msg) {
            handleMemberOnline(msg);
        } else if (msgData instanceof UpdateMemberCombatPowerMessage msg) {
            handleUpdateMemberCombatPower(msg);
        } else if (msgData instanceof UpdateMemberLevelMessage msg) {
            handleUpdateMemberLevel(msg);
        } else if (msgData instanceof GetCombatPowerMessage msg) {
            handleGetCombatPower(msg);
        } else if (msgData instanceof GetDataMessage msg) {
            handleGetData(msg);
        } else if (msgData instanceof DailyResetMessage msg) {
            handleDailyReset(msg);
        } else if (msgData instanceof WeeklyResetMessage msg) {
            handleWeeklyReset(msg);
        }
        // 处理标准消息类型
        else {
            String type = message.getType();
            switch (type) {
                case ActorMessage.TYPE_SAVE -> saveData();
                case ActorMessage.TYPE_STOP -> stop();
                case MessageTypes.ADD_MEMBER -> handleAddMember(message);
                case MessageTypes.REMOVE_MEMBER -> handleRemoveMember(message);
                case MessageTypes.DONATE -> handleDonate(message);
                case MessageTypes.ADD_EXP -> handleAddExp(message);
                case MessageTypes.CHANGE_POSITION -> handleChangePosition(message);
                case MessageTypes.TRANSFER_LEADER -> handleTransferLeader(message);
                case MessageTypes.UPDATE_SETTING -> handleUpdateSetting(message);
                default -> log.warn("未知的消息类型: actorId={}, type={}", getActorId(), type);
            }
        }
    }

    // ==================== 内部服务消息处理 ====================

    private void handleMemberOnline(MemberOnlineMessage msg) {
        data.getMembers().stream()
                .filter(m -> m.getRoleId() == msg.roleId)
                .findFirst()
                .ifPresent(m -> {
                    m.setOnline(msg.online);
                    if (msg.online) {
                        m.setLastLoginTime(System.currentTimeMillis());
                    }
                    markDirty();
                });

        log.debug("公会成员{}线: guildId={}, roleId={}", msg.online ? "上" : "下", getActorId(), msg.roleId);
        msg.future.complete(Result.success());
    }

    private void handleUpdateMemberCombatPower(UpdateMemberCombatPowerMessage msg) {
        data.getMembers().stream()
                .filter(m -> m.getRoleId() == msg.roleId)
                .findFirst()
                .ifPresent(m -> {
                    m.setCombatPower(msg.combatPower);
                    markDirty();
                });

        recalculateTotalCombatPower();
        msg.future.complete(Result.success());
    }

    private void handleUpdateMemberLevel(UpdateMemberLevelMessage msg) {
        data.getMembers().stream()
                .filter(m -> m.getRoleId() == msg.roleId)
                .findFirst()
                .ifPresent(m -> {
                    m.setLevel(msg.level);
                    markDirty();
                });

        msg.future.complete(Result.success());
    }

    private void handleGetCombatPower(GetCombatPowerMessage msg) {
        long totalPower = data.getMembers().stream()
                .mapToLong(GuildData.GuildMember::getCombatPower)
                .sum();
        msg.future.complete(Result.success(totalPower));
    }

    private void handleGetData(GetDataMessage msg) {
        msg.future.complete(Result.success(data));
    }

    private void handleDailyReset(DailyResetMessage msg) {
        String today = LocalDate.now().toString();

        if (today.equals(data.getLastResetDate())) {
            msg.future.complete(Result.success());
            return;
        }

        // 重置成员每日数据
        data.getMembers().forEach(m -> m.setTodayContribution(0));
        data.setLastResetDate(today);
        markDirty();

        log.info("公会每日重置完成: guildId={}", getActorId());
        msg.future.complete(Result.success());
    }

    private void handleWeeklyReset(WeeklyResetMessage msg) {
        data.getMembers().forEach(m -> m.setWeekContribution(0));
        markDirty();

        log.info("公会每周重置完成: guildId={}", getActorId());
        msg.future.complete(Result.success());
    }

    private void recalculateTotalCombatPower() {
        long totalPower = data.getMembers().stream()
                .mapToLong(GuildData.GuildMember::getCombatPower)
                .sum();
        data.setTotalCombatPower(totalPower);
    }

    // ==================== 标准消息处理 ====================

    private void handleAddMember(ActorMessage message) {
        AddMemberRequest req = message.getData();
        if (req == null) return;

        if (data.getMembers().size() >= data.getMaxMember()) {
            if (message.getCallback() != null) {
                message.getCallback().onComplete(false, new RuntimeException("公会已满"));
            }
            return;
        }

        GuildData.GuildMember member = new GuildData.GuildMember();
        member.setRoleId(req.roleId());
        member.setRoleName(req.roleName());
        member.setLevel(req.level());
        member.setAvatarId(req.avatarId());
        member.setPosition(0);
        member.setJoinTime(System.currentTimeMillis());
        member.setCombatPower(req.combatPower());

        data.getMembers().add(member);
        data.setMemberCount(data.getMembers().size());
        recalculateTotalCombatPower();
        markDirty();

        log.info("公会添加成员: guildId={}, roleId={}", getActorId(), req.roleId());

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void handleRemoveMember(ActorMessage message) {
        Long roleId = message.getData();
        if (roleId == null) return;

        boolean removed = data.getMembers().removeIf(m -> m.getRoleId() == roleId);
        if (removed) {
            data.setMemberCount(data.getMembers().size());
            recalculateTotalCombatPower();
            markDirty();
            log.info("公会移除成员: guildId={}, roleId={}", getActorId(), roleId);
        }

        if (message.getCallback() != null) {
            message.getCallback().onComplete(removed, null);
        }
    }

    private void handleDonate(ActorMessage message) {
        DonateRequest req = message.getData();
        if (req == null) return;

        data.setFund(data.getFund() + req.amount());
        long expAdd = req.amount() / 10;
        data.setExp(data.getExp() + expAdd);

        data.getMembers().stream()
                .filter(m -> m.getRoleId() == req.roleId())
                .findFirst()
                .ifPresent(m -> {
                    m.setContribution(m.getContribution() + req.amount());
                    m.setTodayContribution(m.getTodayContribution() + req.amount());
                    m.setWeekContribution(m.getWeekContribution() + req.amount());
                });

        markDirty();
        checkLevelUp();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(req.amount(), null);
        }
    }

    private void handleAddExp(ActorMessage message) {
        Long exp = message.getData();
        if (exp == null || exp <= 0) return;

        data.setExp(data.getExp() + exp);
        markDirty();
        checkLevelUp();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(data.getExp(), null);
        }
    }

    private void handleChangePosition(ActorMessage message) {
        ChangePositionRequest req = message.getData();
        if (req == null) return;

        data.getMembers().stream()
                .filter(m -> m.getRoleId() == req.roleId())
                .findFirst()
                .ifPresent(m -> {
                    m.setPosition(req.newPosition());
                    markDirty();
                });

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void handleTransferLeader(ActorMessage message) {
        TransferLeaderRequest req = message.getData();
        if (req == null) return;

        data.setLeaderId(req.newLeaderId());

        data.getMembers().forEach(m -> {
            if (m.getRoleId() == req.oldLeaderId()) {
                m.setPosition(2); // 副会长
            } else if (m.getRoleId() == req.newLeaderId()) {
                m.setPosition(3); // 会长
            }
        });

        data.getMembers().stream()
                .filter(m -> m.getRoleId() == req.newLeaderId())
                .findFirst()
                .ifPresent(m -> data.setLeaderName(m.getRoleName()));

        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void handleUpdateSetting(ActorMessage message) {
        UpdateSettingRequest req = message.getData();
        if (req == null) return;

        if (req.declaration() != null) {
            data.setDeclaration(req.declaration());
        }
        if (req.joinType() >= 0) {
            data.setJoinType(req.joinType());
        }
        if (req.joinLevel() >= 0) {
            data.setJoinLevel(req.joinLevel());
        }

        markDirty();

        if (message.getCallback() != null) {
            message.getCallback().onComplete(true, null);
        }
    }

    private void checkLevelUp() {
        // TODO: 根据经验表计算是否升级
    }

    // ==================== 消息类型定义 ====================

    /** 成员上下线 */
    public record MemberOnlineMessage(long roleId, boolean online,
                                      CompletableFuture<Result<Void>> future) {}

    /** 更新成员战力 */
    public record UpdateMemberCombatPowerMessage(long roleId, long combatPower,
                                                 CompletableFuture<Result<Void>> future) {}

    /** 更新成员等级 */
    public record UpdateMemberLevelMessage(long roleId, int level,
                                           CompletableFuture<Result<Void>> future) {}

    /** 获取战力 */
    public record GetCombatPowerMessage(CompletableFuture<Result<Long>> future) {}

    /** 获取公会数据 */
    public record GetDataMessage(CompletableFuture<Result<GuildData>> future) {}

    /** 每日重置 */
    public record DailyResetMessage(CompletableFuture<Result<Void>> future) {}

    /** 每周重置 */
    public record WeeklyResetMessage(CompletableFuture<Result<Void>> future) {}

    /** 添加成员 */
    public record AddMemberRequest(long roleId, String roleName, int level, int avatarId, long combatPower) {}

    /** 捐献 */
    public record DonateRequest(long roleId, int donateType, long amount) {}

    /** 修改职位 */
    public record ChangePositionRequest(long roleId, int newPosition) {}

    /** 转让会长 */
    public record TransferLeaderRequest(long oldLeaderId, long newLeaderId) {}

    /** 更新设置 */
    public record UpdateSettingRequest(String declaration, int joinType, int joinLevel) {}

    /**
     * 消息类型常量
     */
    public static class MessageTypes {
        public static final String ADD_MEMBER = "ADD_MEMBER";
        public static final String REMOVE_MEMBER = "REMOVE_MEMBER";
        public static final String DONATE = "DONATE";
        public static final String ADD_EXP = "ADD_EXP";
        public static final String CHANGE_POSITION = "CHANGE_POSITION";
        public static final String TRANSFER_LEADER = "TRANSFER_LEADER";
        public static final String UPDATE_SETTING = "UPDATE_SETTING";
    }
}
