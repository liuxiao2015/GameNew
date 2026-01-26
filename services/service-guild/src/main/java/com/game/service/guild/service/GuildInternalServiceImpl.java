package com.game.service.guild.service;

import com.game.actor.core.ActorSystem;
import com.game.api.guild.GuildDTO;
import com.game.entity.document.GuildData;
import com.game.service.guild.actor.GuildActor;
import com.game.api.guild.GuildInternalService;
import com.game.api.guild.GuildMemberDTO;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.mongo.MongoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公会内部服务实现
 * <p>
 * 提供给其他服务 (Game/Task/Rank) 内部调用
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class GuildInternalServiceImpl implements GuildInternalService {

    private final MongoService mongoService;

    @Autowired(required = false)
    @Qualifier("guildActorSystem")
    private ActorSystem<GuildActor> guildActorSystem;

    // ==================== 玩家相关回调 ====================

    @Override
    public Result<Void> onPlayerOnline(long roleId, long guildId) {
        if (guildId <= 0) {
            return Result.success();
        }
        if (guildActorSystem == null) {
            log.warn("guildActorSystem 未注入");
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }

        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.MemberOnlineMessage(roleId, true, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("通知公会成员上线失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> onPlayerOffline(long roleId, long guildId) {
        if (guildId <= 0) {
            return Result.success();
        }
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }

        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.MemberOnlineMessage(roleId, false, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("通知公会成员下线失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> updateMemberCombatPower(long roleId, long guildId, long combatPower) {
        if (guildId <= 0) {
            return Result.success();
        }
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }

        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.UpdateMemberCombatPowerMessage(
                    roleId, combatPower, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("更新公会成员战力失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> updateMemberLevel(long roleId, long guildId, int level) {
        if (guildId <= 0) {
            return Result.success();
        }
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }

        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.UpdateMemberLevelMessage(
                    roleId, level, future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("更新公会成员等级失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ==================== 排行相关 ====================

    @Override
    public Result<Long> getGuildCombatPower(long guildId) {
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Long>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.GetCombatPowerMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取公会战力失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<List<GuildDTO>> batchGetGuilds(List<Long> guildIds) {
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        List<GuildDTO> results = new ArrayList<>();
        
        for (Long guildId : guildIds) {
            try {
                CompletableFuture<Result<GuildData>> future = new CompletableFuture<>();
                guildActorSystem.tell(guildId, new GuildActor.GetDataMessage(future));
                
                Result<GuildData> result = future.get(3, TimeUnit.SECONDS);
                if (result.isSuccess() && result.getData() != null) {
                    results.add(toGuildDTO(result.getData()));
                }
            } catch (Exception e) {
                log.warn("获取公会信息失败: guildId={}", guildId, e);
            }
        }
        
        return Result.success(results);
    }

    // ==================== 定时任务相关 ====================

    @Override
    public Result<Void> dailyReset(long guildId) {
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.DailyResetMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("公会每日重置失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> weeklyReset(long guildId) {
        if (guildActorSystem == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
        try {
            CompletableFuture<Result<Void>> future = new CompletableFuture<>();
            
            guildActorSystem.tell(guildId, new GuildActor.WeeklyResetMessage(future));
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("公会每周重置失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<List<Long>> getAllGuildIds() {
        try {
            // 从 MongoDB 查询所有公会 ID
            List<GuildData> guilds = mongoService.findAll(GuildData.class);
            List<Long> guildIds = guilds.stream()
                    .map(GuildData::getGuildId)
                    .toList();
            return Result.success(guildIds);
        } catch (Exception e) {
            log.error("获取所有公会 ID 失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ==================== 数据转换 ====================

    private GuildDTO toGuildDTO(GuildData data) {
        List<GuildMemberDTO> members = data.getMembers().stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList());

        return GuildDTO.builder()
                .guildId(data.getGuildId())
                .guildName(data.getGuildName())
                .level(data.getLevel())
                .exp(data.getExp())
                .fund(data.getFund())
                .leaderId(data.getLeaderId())
                .leaderName(data.getLeaderName())
                .memberCount(data.getMemberCount())
                .maxMember(data.getMaxMember())
                .declaration(data.getDeclaration())
                .members(members)
                .build();
    }

    private GuildMemberDTO toMemberDTO(GuildData.GuildMember member) {
        return GuildMemberDTO.builder()
                .roleId(member.getRoleId())
                .roleName(member.getRoleName())
                .level(member.getLevel())
                .avatarId(member.getAvatarId())
                .position(member.getPosition())
                .contribution(member.getContribution())
                .todayContribution(member.getTodayContribution())
                .combatPower(member.getCombatPower())
                .online(member.isOnline())
                .lastLoginTime(member.getLastLoginTime())
                .joinTime(member.getJoinTime())
                .build();
    }
}
