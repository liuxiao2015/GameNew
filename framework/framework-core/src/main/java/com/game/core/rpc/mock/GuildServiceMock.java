package com.game.core.rpc.mock;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildMemberDTO;
import com.game.api.guild.GuildService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 公会服务降级 Mock
 * <p>
 * 当 GuildService 不可用时，提供降级响应。
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class GuildServiceMock implements GuildService {

    private static final String SERVICE_UNAVAILABLE = "公会服务暂时不可用";

    @Override
    public Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId) {
        log.warn("[降级] GuildService.createGuild 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<GuildDTO> getGuildInfo(long guildId) {
        log.warn("[降级] GuildService.getGuildInfo 服务不可用: guildId={}", guildId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        log.warn("[降级] GuildService.getPlayerGuild 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<List<GuildDTO>> searchGuild(String keyword, int page, int size) {
        log.warn("[降级] GuildService.searchGuild 服务不可用: keyword={}", keyword);
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<Void> applyJoinGuild(long roleId, long guildId, String message) {
        log.warn("[降级] GuildService.applyJoinGuild 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> joinGuild(long roleId, long guildId) {
        log.warn("[降级] GuildService.joinGuild 服务不可用: roleId={}, guildId={}", roleId, guildId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> handleApply(long operatorId, long applyId, boolean accept) {
        log.warn("[降级] GuildService.handleApply 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> leaveGuild(long roleId) {
        log.warn("[降级] GuildService.leaveGuild 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> kickMember(long operatorId, long targetId) {
        log.warn("[降级] GuildService.kickMember 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Long> donate(long roleId, int donateType, long amount) {
        log.warn("[降级] GuildService.donate 服务不可用: roleId={}", roleId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> changeMemberPosition(long operatorId, long targetId, int newPosition) {
        log.warn("[降级] GuildService.changeMemberPosition 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> transferLeader(long currentLeaderId, long newLeaderId) {
        log.warn("[降级] GuildService.transferLeader 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Void> dissolveGuild(long leaderId) {
        log.warn("[降级] GuildService.dissolveGuild 服务不可用: leaderId={}", leaderId);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<List<GuildMemberDTO>> getMembers(long guildId) {
        log.warn("[降级] GuildService.getMembers 服务不可用: guildId={}", guildId);
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<Void> dailyReset() {
        log.warn("[降级] GuildService.dailyReset 服务不可用");
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }
}
