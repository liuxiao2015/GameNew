package com.game.service.game.service;

import com.game.actor.core.ActorSystem;
import com.game.api.player.PlayerDTO;
import com.game.entity.player.PlayerData;
import com.game.service.game.actor.PlayerActor;
import com.game.api.player.PlayerInternalService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 玩家内部服务实现
 * <p>
 * 提供给其他服务 (Guild/Task/Rank) 内部调用
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class PlayerInternalServiceImpl implements PlayerInternalService {

    private static final String ONLINE_KEY = "online:players";

    private final RedisService redisService;

    @Autowired(required = false)
    @Qualifier("playerActorSystem")
    private ActorSystem<PlayerActor> playerActorSystem;

    // ==================== 公会相关回调 ====================

    @Override
    public Result<Void> setPlayerGuild(long roleId, long guildId, String guildName, int position) {
        if (playerActorSystem == null) {
            log.warn("playerActorSystem 未注入");
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.SetGuildMessage(
                    guildId, guildName, position, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("设置玩家公会信息失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> deductCurrency(long roleId, int currencyType, long amount, String reason) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.DeductCurrencyMessage(
                    currencyType, amount, reason, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("扣除玩家货币失败: roleId={}, type={}, amount={}", roleId, currencyType, amount, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Boolean> checkCurrency(long roleId, int currencyType, long amount) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Boolean>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.CheckCurrencyMessage(
                    currencyType, amount, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("检查玩家货币失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ==================== 排行相关 ====================

    @Override
    public Result<Long> getPlayerCombatPower(long roleId) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Long>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.GetCombatPowerMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取玩家战力失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<List<PlayerDTO>> batchGetPlayers(List<Long> roleIds) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        List<PlayerDTO> results = new ArrayList<>();
        
        for (Long roleId : roleIds) {
            try {
                CompletableFuture<Result<PlayerData>> future = new CompletableFuture<>();
                playerActorSystem.tell(roleId, new PlayerActor.GetDataMessage(future));
                
                Result<PlayerData> result = future.get(3, TimeUnit.SECONDS);
                if (result.isSuccess() && result.getData() != null) {
                    results.add(toPlayerDTO(result.getData()));
                }
            } catch (Exception e) {
                log.warn("获取玩家信息失败: roleId={}", roleId, e);
            }
        }
        
        return Result.success(results);
    }

    // ==================== 任务/活动相关 ====================

    @Override
    public Result<Void> dailyReset(long roleId) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.DailyResetMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("玩家每日重置失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> weeklyReset(long roleId) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.WeeklyResetMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("玩家每周重置失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> sendMail(long roleId, String title, String content, Map<Integer, Long> attachments) {
        if (playerActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            playerActorSystem.tell(roleId, new PlayerActor.SendMailMessage(
                    title, content, attachments, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("发送邮件失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
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
