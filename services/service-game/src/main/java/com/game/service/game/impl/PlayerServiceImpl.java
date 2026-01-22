package com.game.service.game.impl;

import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import com.game.api.player.PlayerDTO;
import com.game.entity.player.PlayerData;
import com.game.service.game.actor.PlayerActor;
import com.game.service.game.actor.PlayerActor.MessageTypes;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.game.log.operation.OperationLog;
import com.game.log.operation.OperationLogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 玩家服务实现
 *
 * @author GameServer
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final RedisService redisService;
    private final MongoService mongoService;
    private final OperationLogService operationLogService;

    @Value("${game.actor.player.max-size:10000}")
    private int maxSize;

    @Value("${game.actor.player.idle-timeout-minutes:30}")
    private int idleTimeoutMinutes;

    @Value("${game.actor.player.save-interval-seconds:300}")
    private int saveIntervalSeconds;

    /**
     * 玩家 Actor 系统
     */
    private ActorSystem<PlayerActor> playerActorSystem;

    @PostConstruct
    public void init() {
        // 初始化 Actor 系统
        ActorSystem.ActorSystemConfig config = ActorSystem.ActorSystemConfig.create()
                .maxSize(maxSize)
                .idleTimeoutMinutes(idleTimeoutMinutes)
                .saveIntervalSeconds(saveIntervalSeconds);

        playerActorSystem = new ActorSystem<>(
                "PlayerActorSystem",
                config,
                roleId -> new PlayerActor(roleId, redisService, mongoService)
        );
        playerActorSystem.init();

        log.info("玩家服务初始化完成: maxSize={}, idleTimeout={}min, saveInterval={}s",
                maxSize, idleTimeoutMinutes, saveIntervalSeconds);
    }

    @Override
    public Result<PlayerDTO> getPlayerInfo(long roleId) {
        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null || actor.getData() == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        return Result.success(toPlayerDTO(actor.getData()));
    }

    @Override
    public Result<Void> addExp(long roleId, long exp, String reason) {
        log.debug("增加经验: roleId={}, exp={}, reason={}", roleId, exp, reason);

        if (exp <= 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "经验值必须大于0");
        }

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(
                MessageTypes.ADD_EXP,
                new PlayerActor.AddExpRequest(exp, reason)
        );

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        // 记录操作日志
        PlayerData data = actor.getData();
        operationLogService.log(roleId, data.getRoleName(), 
                OperationLog.TYPE_LEVEL_UP, "ADD_EXP",
                data.getExp(), data.getExp() + exp, exp, reason);

        return Result.success();
    }

    @Override
    public Result<Void> addGold(long roleId, long gold, String reason) {
        log.debug("增加金币: roleId={}, gold={}, reason={}", roleId, gold, reason);

        if (gold == 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "金币数量不能为0");
        }

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        PlayerData data = actor.getData();

        // 检查金币是否足够（如果是扣除）
        if (gold < 0 && data.getGold() + gold < 0) {
            return Result.fail(ErrorCode.GOLD_NOT_ENOUGH);
        }

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(
                MessageTypes.ADD_GOLD,
                new PlayerActor.AddGoldRequest(gold, reason)
        );

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        // 记录操作日志
        operationLogService.log(roleId, data.getRoleName(),
                OperationLog.TYPE_GOLD_CHANGE, gold > 0 ? "ADD" : "CONSUME",
                data.getGold(), data.getGold() + gold, gold, reason);

        return Result.success();
    }

    @Override
    public Result<Void> addDiamond(long roleId, long diamond, String reason) {
        log.debug("增加钻石: roleId={}, diamond={}, reason={}", roleId, diamond, reason);

        if (diamond == 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "钻石数量不能为0");
        }

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        PlayerData data = actor.getData();

        // 检查钻石是否足够（如果是扣除）
        if (diamond < 0 && data.getDiamond() + diamond < 0) {
            return Result.fail(ErrorCode.DIAMOND_NOT_ENOUGH);
        }

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(
                MessageTypes.ADD_DIAMOND,
                new PlayerActor.AddDiamondRequest(diamond, reason)
        );

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        // 记录操作日志
        operationLogService.log(roleId, data.getRoleName(),
                OperationLog.TYPE_DIAMOND_CHANGE, diamond > 0 ? "ADD" : "CONSUME",
                data.getDiamond(), data.getDiamond() + diamond, diamond, reason);

        return Result.success();
    }

    @Override
    public Result<Void> addItem(long roleId, int itemId, long count, String reason) {
        log.debug("增加物品: roleId={}, itemId={}, count={}, reason={}", roleId, itemId, count, reason);

        if (itemId <= 0 || count <= 0) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(
                MessageTypes.ADD_ITEM,
                new PlayerActor.AddItemRequest(itemId, count, reason)
        );

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> changeName(long roleId, String newName) {
        log.info("修改名字: roleId={}, newName={}", roleId, newName);

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 检查名字是否重复

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(MessageTypes.CHANGE_NAME, newName);

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> changeAvatar(long roleId, int avatarId) {
        log.debug("修改头像: roleId={}, avatarId={}", roleId, avatarId);

        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // 发送消息到 Actor
        ActorMessage message = ActorMessage.of(MessageTypes.CHANGE_AVATAR, avatarId);

        boolean sent = actor.tell(message);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> banPlayer(long roleId, long duration, String reason) {
        log.info("封禁玩家: roleId={}, duration={}, reason={}", roleId, duration, reason);

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor != null) {
            PlayerData data = actor.getData();
            data.setBanEndTime(duration == 0 ? -1 : System.currentTimeMillis() + duration * 1000);
            data.setBanReason(reason);
            actor.markDirty();
        }

        // TODO: 直接更新数据库

        return Result.success();
    }

    @Override
    public Result<Void> unbanPlayer(long roleId) {
        log.info("解封玩家: roleId={}", roleId);

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor != null) {
            PlayerData data = actor.getData();
            data.setBanEndTime(0);
            data.setBanReason(null);
            actor.markDirty();
        }

        // TODO: 直接更新数据库

        return Result.success();
    }

    /**
     * 获取 Actor 系统 (供其他模块使用)
     */
    public ActorSystem<PlayerActor> getPlayerActorSystem() {
        return playerActorSystem;
    }

    /**
     * 转换为 DTO
     */
    private PlayerDTO toPlayerDTO(PlayerData data) {
        PlayerDTO dto = new PlayerDTO();
        dto.setRoleId(data.getRoleId());
        dto.setRoleName(data.getRoleName());
        dto.setLevel(data.getLevel());
        dto.setExp(data.getExp());
        dto.setGold(data.getGold());
        dto.setDiamond(data.getDiamond());
        dto.setVipLevel(data.getVipLevel());
        dto.setVipExp(data.getVipExp());
        dto.setAvatarId(data.getAvatarId());
        dto.setFrameId(data.getFrameId());
        dto.setEnergy(data.getEnergy());
        dto.setMaxEnergy(data.getMaxEnergy());
        dto.setGuildId(data.getGuildId());
        dto.setGuildName(data.getGuildName());
        dto.setCombatPower(data.getCombatPower());
        dto.setSignature(data.getSignature());
        if (data.getCreateTime() != null) {
            dto.setCreateTime(data.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        dto.setLastLoginTime(data.getLastLoginTime());
        dto.setBanned(data.isBanned());
        dto.setBanEndTime(data.getBanEndTime());
        return dto;
    }
}
