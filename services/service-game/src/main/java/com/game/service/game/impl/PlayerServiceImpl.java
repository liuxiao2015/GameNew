package com.game.service.game.impl;

import com.game.api.player.PlayerDTO;
import com.game.entity.document.PlayerData;
import com.game.service.game.actor.PlayerActor;
import com.game.service.game.actor.PlayerActorSystem;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 玩家服务实现
 * <p>
 * 使用 PlayerActorSystem 管理玩家 Actor
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerActorSystem playerActorSystem;

    @Override
    public Result<PlayerDTO> getPlayerInfo(long roleId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
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

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // 使用新的消息格式
        actor.fire("ADD_EXP", new PlayerActor.AddExpData(exp, reason));

        return Result.success();
    }

    @Override
    public Result<Void> addGold(long roleId, long gold, String reason) {
        log.debug("增加金币: roleId={}, gold={}, reason={}", roleId, gold, reason);

        if (gold == 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "金币数量不能为0");
        }

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        PlayerData data = actor.getData();

        // 检查金币是否足够（如果是扣除）
        if (gold < 0 && data.getGold() + gold < 0) {
            return Result.fail(ErrorCode.GOLD_NOT_ENOUGH);
        }

        // 使用新的消息格式
        actor.fire("ADD_GOLD", new PlayerActor.AddGoldData(gold, reason));

        return Result.success();
    }

    @Override
    public Result<Void> addDiamond(long roleId, long diamond, String reason) {
        log.debug("增加钻石: roleId={}, diamond={}, reason={}", roleId, diamond, reason);

        if (diamond == 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "钻石数量不能为0");
        }

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        PlayerData data = actor.getData();

        // 检查钻石是否足够（如果是扣除）
        if (diamond < 0 && data.getDiamond() + diamond < 0) {
            return Result.fail(ErrorCode.DIAMOND_NOT_ENOUGH);
        }

        // 使用新的消息格式
        actor.fire("ADD_DIAMOND", new PlayerActor.AddDiamondData(diamond, reason));

        return Result.success();
    }

    @Override
    public Result<Void> addItem(long roleId, int itemId, long count, String reason) {
        log.debug("增加物品: roleId={}, itemId={}, count={}, reason={}", roleId, itemId, count, reason);

        if (itemId <= 0 || count <= 0) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 实现添加物品消息
        // actor.fire("ADD_ITEM", new PlayerActor.AddItemData(itemId, count, reason));

        return Result.success();
    }

    @Override
    public Result<Void> changeName(long roleId, String newName) {
        log.info("修改名字: roleId={}, newName={}", roleId, newName);

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 实现改名消息
        // actor.fire("CHANGE_NAME", newName);

        return Result.success();
    }

    @Override
    public Result<Void> changeAvatar(long roleId, int avatarId) {
        log.debug("修改头像: roleId={}, avatarId={}", roleId, avatarId);

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 实现修改头像消息
        // actor.fire("CHANGE_AVATAR", avatarId);

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

        return Result.success();
    }

    @Override
    public Result<Void> setGuildInfo(long roleId, long guildId, String guildName, int position) {
        log.info("设置公会信息: roleId={}, guildId={}, guildName={}, position={}", 
                roleId, guildId, guildName, position);

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("SET_GUILD", new PlayerActor.SetGuildData(guildId, guildName, position));
        }

        return Result.success();
    }

    @Override
    public Result<Void> dailyReset() {
        log.info("执行玩家每日重置");
        // 实际实现：遍历所有在线玩家执行每日重置
        // 这里简化处理，实际应该通过批量任务处理
        return Result.success();
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
