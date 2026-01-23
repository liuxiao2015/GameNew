package com.game.service.guild.handler;

import com.game.actor.core.ActorSystem;
import com.game.api.common.MethodId;
import com.game.api.common.ProtocolConstants;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.service.guild.actor.GuildActor;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 公会协议处理器
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_GUILD, value = "公会模块")
@RequiredArgsConstructor
public class GuildHandler extends BaseHandler {

    @Qualifier("guildActorSystem")
    private final ActorSystem<GuildActor> guildActorSystem;

    /**
     * 创建公会
     */
    @Protocol(methodId = MethodId.Guild.CREATE, desc = "创建公会")
    public Message createGuild(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("创建公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 获取公会信息
     */
    @Protocol(methodId = MethodId.Guild.GET_INFO, desc = "获取公会信息")
    public Message getGuildInfo(Session session) {
        long roleId = session.getRoleId();
        log.debug("获取公会信息: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 搜索公会
     */
    @Protocol(methodId = MethodId.Guild.SEARCH, desc = "搜索公会")
    public Message searchGuild(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.debug("搜索公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 申请加入公会
     */
    @Protocol(methodId = MethodId.Guild.APPLY_JOIN, desc = "申请加入公会")
    public Message applyJoin(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("申请加入公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 处理加入申请
     */
    @Protocol(methodId = MethodId.Guild.HANDLE_APPLY, desc = "处理加入申请")
    public Message handleApply(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("处理公会申请: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 退出公会
     */
    @Protocol(methodId = MethodId.Guild.LEAVE, desc = "退出公会")
    public Message leaveGuild(Session session) {
        long roleId = session.getRoleId();
        log.info("退出公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 踢出成员
     */
    @Protocol(methodId = MethodId.Guild.KICK_MEMBER, desc = "踢出成员")
    public Message kickMember(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("踢出公会成员: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 公会捐献
     */
    @Protocol(methodId = MethodId.Guild.DONATE, desc = "公会捐献")
    public Message donate(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("公会捐献: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 修改成员职位
     */
    @Protocol(methodId = MethodId.Guild.CHANGE_POSITION, desc = "修改成员职位")
    public Message changePosition(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("修改公会成员职位: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 转让会长
     */
    @Protocol(methodId = MethodId.Guild.TRANSFER_LEADER, desc = "转让会长")
    public Message transferLeader(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("转让会长: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 修改公会设置
     */
    @Protocol(methodId = MethodId.Guild.CHANGE_SETTING, desc = "修改公会设置")
    public Message changeSetting(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("修改公会设置: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }
}
