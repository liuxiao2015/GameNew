package com.game.service.game.service;

import com.game.api.player.PlayerDTO;
import com.game.entity.document.PlayerData;
import com.game.service.game.actor.PlayerActor;
import com.game.service.game.actor.PlayerActorSystem;
import com.game.api.player.PlayerInternalService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 玩家内部服务实现
 * <p>
 * 提供给其他服务 (Guild/Task/Rank) 内部调用
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class PlayerInternalServiceImpl implements PlayerInternalService {

    private static final String ONLINE_KEY = "online:players";

    private final RedisService redisService;
    private final PlayerActorSystem playerActorSystem;

    // ==================== 公会相关回调 ====================

    @Override
    public Result<Void> setPlayerGuild(long roleId, long guildId, String guildName, int position) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        actor.fire("SET_GUILD", new PlayerActor.SetGuildData(guildId, guildName, position));
        return Result.success();
    }

    @Override
    public Result<Void> deductCurrency(long roleId, int currencyType, long amount, String reason) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 根据货币类型扣除
        if (currencyType == 1) { // 金币
            actor.fire("ADD_GOLD", new PlayerActor.AddGoldData(-amount, reason));
        } else if (currencyType == 2) { // 钻石
            actor.fire("ADD_DIAMOND", new PlayerActor.AddDiamondData(-amount, reason));
        }
        
        return Result.success();
    }

    @Override
    public Result<Boolean> checkCurrency(long roleId, int currencyType, long amount) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        PlayerData data = actor.getData();
        boolean enough = false;
        
        if (currencyType == 1) { // 金币
            enough = data.getGold() >= amount;
        } else if (currencyType == 2) { // 钻石
            enough = data.getDiamond() >= amount;
        }
        
        return Result.success(enough);
    }

    // ==================== 排行相关 ====================

    @Override
    public Result<Long> getPlayerCombatPower(long roleId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        return Result.success(actor.getData().getCombatPower());
    }

    @Override
    public Result<List<PlayerDTO>> batchGetPlayers(List<Long> roleIds) {
        List<PlayerDTO> results = new ArrayList<>();
        
        for (Long roleId : roleIds) {
            PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
            if (actor != null && actor.getData() != null) {
                results.add(toPlayerDTO(actor.getData()));
            }
        }
        
        return Result.success(results);
    }

    // ==================== 任务/活动相关 ====================

    @Override
    public Result<Void> dailyReset(long roleId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // TODO: 实现每日重置逻辑
        actor.fire("DAILY_RESET");
        
        return Result.success();
    }

    @Override
    public Result<Void> weeklyReset(long roleId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // TODO: 实现每周重置逻辑
        actor.fire("WEEKLY_RESET");
        
        return Result.success();
    }

    @Override
    public Result<Void> sendMail(long roleId, String title, String content, Map<Integer, Long> attachments) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // TODO: 实现发送邮件逻辑
        // actor.fire("SEND_MAIL", new PlayerActor.SendMailData(title, content, attachments));
        
        return Result.success();
    }

    // ==================== 在线状态 ====================

    @Override
    public Result<Boolean> isOnline(long roleId) {
        boolean online = redisService.sIsMember(ONLINE_KEY, String.valueOf(roleId));
        return Result.success(online);
    }

    @Override
    public Result<List<Long>> getOnlinePlayers() {
        Set<String> members = redisService.sMembers(ONLINE_KEY);
        List<Long> result = new ArrayList<>();
        
        if (members != null) {
            for (String member : members) {
                try {
                    result.add(Long.parseLong(member));
                } catch (NumberFormatException e) {
                    // 忽略无效数据
                }
            }
        }
        
        return Result.success(result);
    }

    // ==================== 数据转换 ====================

    private PlayerDTO toPlayerDTO(PlayerData data) {
        return PlayerDTO.builder()
                .roleId(data.getRoleId())
                .roleName(data.getRoleName())
                .level(data.getLevel())
                .exp(data.getExp())
                .vipLevel(data.getVipLevel())
                .vipExp(data.getVipExp())
                .avatarId(data.getAvatarId())
                .frameId(data.getFrameId())
                .gold(data.getGold())
                .diamond(data.getDiamond())
                .bindDiamond(data.getBindDiamond())
                .combatPower(data.getCombatPower())
                .energy(data.getEnergy())
                .maxEnergy(data.getMaxEnergy())
                .guildId(data.getGuildId())
                .guildName(data.getGuildName())
                .guildPosition(data.getGuildPosition())
                .serverId(data.getServerId())
                .signature(data.getSignature())
                .lastLoginTime(data.getLastLoginTime())
                .banned(data.isBanned())
                .banEndTime(data.getBanEndTime())
                .build();
    }
}
