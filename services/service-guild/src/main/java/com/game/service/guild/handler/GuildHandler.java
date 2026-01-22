package com.game.service.guild.handler;

import com.game.actor.core.ActorSystem;
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
     * 获取公会信息
     */
    @Protocol(methodId = 0x01, desc = "获取公会信息")
    public Message getGuildInfo(Session session) {
        long roleId = session.getRoleId();
        log.debug("获取公会信息: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 创建公会
     */
    @Protocol(methodId = 0x02, desc = "创建公会")
    public Message createGuild(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("创建公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 申请加入公会
     */
    @Protocol(methodId = 0x03, desc = "申请加入公会")
    public Message applyJoin(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("申请加入公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 退出公会
     */
    @Protocol(methodId = 0x04, desc = "退出公会")
    public Message leaveGuild(Session session) {
        long roleId = session.getRoleId();
        log.info("退出公会: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 获取公会成员列表
     */
    @Protocol(methodId = 0x05, desc = "获取成员列表")
    public Message getMemberList(Session session) {
        long roleId = session.getRoleId();
        log.debug("获取公会成员列表: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 公会捐献
     */
    @Protocol(methodId = 0x10, desc = "公会捐献")
    public Message donate(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("公会捐献: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 审批申请
     */
    @Protocol(methodId = 0x20, desc = "审批申请")
    public Message approveApply(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("审批公会申请: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 踢出成员
     */
    @Protocol(methodId = 0x21, desc = "踢出成员")
    public Message kickMember(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("踢出公会成员: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }
}
