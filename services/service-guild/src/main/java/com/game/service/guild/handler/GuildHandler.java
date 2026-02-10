package com.game.service.guild.handler;

import com.game.api.common.MethodId;
import com.game.api.common.ProtocolConstants;
import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;
import com.game.common.result.Result;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.proto.*;
import com.game.service.guild.service.GuildBusinessService;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公会协议处理器
 * <p>
 * 处理所有公会相关的客户端请求
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - ProtocolController: 模块级协议处理器声明
 * - Protocol: 方法级协议处理声明
 * - BaseHandler: 通用处理器基类
 * - GameException: 业务异常统一处理
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_GUILD, value = "公会模块")
@RequiredArgsConstructor
public class GuildHandler extends BaseHandler {

    private final GuildBusinessService guildBusinessService;

    /**
     * 创建公会
     */
    @Protocol(methodId = MethodId.Guild.CREATE, desc = "创建公会", requireLogin = true, requireRole = true)
    public Message createGuild(Session session, C2S_CreateGuild request) {
        long roleId = session.getRoleId();
        log.info("创建公会: roleId={}, guildName={}", roleId, request.getGuildName());

        Result<GuildInfo> result = guildBusinessService.createGuild(
                roleId,
                request.getGuildName(),
                request.getDeclaration(),
                request.getIconId()
        );

        if (!result.isSuccess()) {
            return S2C_CreateGuild.newBuilder()
                    .setResult(buildErrorResult(result.getCode(), result.getMessage()))
                    .build();
        }

        return S2C_CreateGuild.newBuilder()
                .setResult(buildSuccessResult())
                .setGuild(result.getData())
                .build();
    }

    /**
     * 获取公会信息
     */
    @Protocol(methodId = MethodId.Guild.GET_INFO, desc = "获取公会信息", requireLogin = true, requireRole = true)
    public Message getGuildInfo(Session session, C2S_GetGuildInfo request) {
        long roleId = session.getRoleId();
        long guildId = request.getGuildId();
        log.debug("获取公会信息: roleId={}, guildId={}", roleId, guildId);

        // TODO: 实现获取公会信息逻辑
        return S2C_GetGuildInfo.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 搜索公会
     */
    @Protocol(methodId = MethodId.Guild.SEARCH, desc = "搜索公会", requireLogin = true, requireRole = true)
    public Message searchGuild(Session session, C2S_SearchGuild request) {
        long roleId = session.getRoleId();
        log.debug("搜索公会: roleId={}, keyword={}", roleId, request.getKeyword());

        // TODO: 实现搜索公会逻辑
        return S2C_SearchGuild.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 申请加入公会
     */
    @Protocol(methodId = MethodId.Guild.APPLY_JOIN, desc = "申请加入公会", requireLogin = true, requireRole = true)
    public Message applyJoin(Session session, C2S_ApplyJoinGuild request) {
        long roleId = session.getRoleId();
        log.info("申请加入公会: roleId={}, guildId={}", roleId, request.getGuildId());

        Result<Void> result = guildBusinessService.applyJoinGuild(
                roleId,
                request.getGuildId(),
                request.getMessage()
        );

        return S2C_ApplyJoinGuild.newBuilder()
                .setResult(result.isSuccess() ? 
                        buildSuccessResult() : 
                        buildErrorResult(result.getCode(), result.getMessage()))
                .build();
    }

    /**
     * 处理加入申请
     */
    @Protocol(methodId = MethodId.Guild.HANDLE_APPLY, desc = "处理加入申请", requireLogin = true, requireRole = true)
    public Message handleApply(Session session, C2S_HandleGuildApply request) {
        long roleId = session.getRoleId();
        log.info("处理公会申请: roleId={}, applyId={}, accept={}", 
                roleId, request.getApplyId(), request.getAccept());

        // TODO: 实现处理申请逻辑
        return S2C_HandleGuildApply.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 退出公会
     */
    @Protocol(methodId = MethodId.Guild.LEAVE, desc = "退出公会", requireLogin = true, requireRole = true)
    public Message leaveGuild(Session session, C2S_LeaveGuild request) {
        long roleId = session.getRoleId();
        log.info("退出公会: roleId={}", roleId);

        Result<Void> result = guildBusinessService.leaveGuild(roleId);

        return S2C_LeaveGuild.newBuilder()
                .setResult(result.isSuccess() ? 
                        buildSuccessResult() : 
                        buildErrorResult(result.getCode(), result.getMessage()))
                .build();
    }

    /**
     * 踢出成员
     */
    @Protocol(methodId = MethodId.Guild.KICK_MEMBER, desc = "踢出成员", requireLogin = true, requireRole = true)
    public Message kickMember(Session session, C2S_KickGuildMember request) {
        long roleId = session.getRoleId();
        log.info("踢出公会成员: roleId={}, targetId={}", roleId, request.getRoleId());

        // TODO: 实现踢出成员逻辑
        return S2C_KickGuildMember.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 公会捐献
     */
    @Protocol(methodId = MethodId.Guild.DONATE, desc = "公会捐献", requireLogin = true, requireRole = true)
    public Message donate(Session session, C2S_GuildDonate request) {
        long roleId = session.getRoleId();
        log.info("公会捐献: roleId={}, type={}, amount={}", 
                roleId, request.getDonateType(), request.getAmount());

        Result<GuildBusinessService.GuildDonateResult> result = 
                guildBusinessService.donate(roleId, request.getDonateType(), request.getAmount());

        if (!result.isSuccess()) {
            return S2C_GuildDonate.newBuilder()
                    .setResult(buildErrorResult(result.getCode(), result.getMessage()))
                    .build();
        }

        return S2C_GuildDonate.newBuilder()
                .setResult(buildSuccessResult())
                .setContribution(result.getData().contribution())
                .setGuildExp(result.getData().guildExp())
                .build();
    }

    /**
     * 修改成员职位
     */
    @Protocol(methodId = MethodId.Guild.CHANGE_POSITION, desc = "修改成员职位", requireLogin = true, requireRole = true)
    public Message changePosition(Session session, C2S_ChangeGuildPosition request) {
        long roleId = session.getRoleId();
        log.info("修改公会成员职位: roleId={}, targetId={}, newPosition={}", 
                roleId, request.getRoleId(), request.getNewPosition());

        // TODO: 实现修改职位逻辑
        return S2C_ChangeGuildPosition.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 转让会长
     */
    @Protocol(methodId = MethodId.Guild.TRANSFER_LEADER, desc = "转让会长", requireLogin = true, requireRole = true)
    public Message transferLeader(Session session, C2S_TransferGuildLeader request) {
        long roleId = session.getRoleId();
        log.info("转让会长: roleId={}, newLeaderId={}", roleId, request.getNewLeaderId());

        // TODO: 实现转让会长逻辑
        return S2C_TransferGuildLeader.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 修改公会设置
     */
    @Protocol(methodId = MethodId.Guild.CHANGE_SETTING, desc = "修改公会设置", requireLogin = true, requireRole = true)
    public Message changeSetting(Session session, C2S_ChangeGuildSetting request) {
        long roleId = session.getRoleId();
        log.info("修改公会设置: roleId={}", roleId);

        // TODO: 实现修改设置逻辑
        return S2C_ChangeGuildSetting.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    // ==================== 辅助方法 ====================

    private com.game.proto.Result buildSuccessResult() {
        return com.game.proto.Result.newBuilder()
                .setCode(0)
                .setMessage("success")
                .build();
    }

    private com.game.proto.Result buildErrorResult(int code, String message) {
        return com.game.proto.Result.newBuilder()
                .setCode(code)
                .setMessage(message != null ? message : "error")
                .build();
    }
}
