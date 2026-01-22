package com.game.service.game.handler;

import com.game.actor.core.ActorSystem;
import com.game.api.common.ProtocolConstants;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.service.game.actor.PlayerActor;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 玩家协议处理器
 * <p>
 * 所有玩家相关的客户端请求都通过此处理器分发到 PlayerActor
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_PLAYER, value = "玩家模块")
@RequiredArgsConstructor
public class PlayerHandler extends BaseHandler {

    @Qualifier("playerActorSystem")
    private final ActorSystem<PlayerActor> playerActorSystem;

    /**
     * 获取玩家信息
     */
    @Protocol(methodId = 0x01, desc = "获取玩家信息")
    public Message getPlayerInfo(Session session) {
        long roleId = session.getRoleId();
        log.debug("获取玩家信息: roleId={}", roleId);

        // 通过 Actor 获取玩家数据
        // PlayerActor actor = playerActorSystem.getActor(roleId);
        // PlayerData data = actor.getData();
        // return PlayerInfoResponse.newBuilder()...build();

        // TODO: 实际实现
        return null;
    }

    /**
     * 修改名字
     */
    @Protocol(methodId = 0x02, desc = "修改角色名")
    public Message changeName(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("修改角色名: roleId={}", roleId);

        // 发送消息到 Actor
        // String newName = ChangeNameRequest.parseFrom(requestData).getNewName();
        // playerActorSystem.tell(roleId, 
        //     ActorMessage.of(PlayerActor.MessageTypes.CHANGE_NAME, newName));

        // TODO: 实际实现
        return null;
    }

    /**
     * 修改头像
     */
    @Protocol(methodId = 0x03, desc = "修改头像")
    public Message changeAvatar(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.info("修改头像: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 使用物品
     */
    @Protocol(methodId = 0x10, desc = "使用物品")
    public Message useItem(Session session, byte[] requestData) {
        long roleId = session.getRoleId();
        log.debug("使用物品: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }

    /**
     * 获取背包信息
     */
    @Protocol(methodId = 0x11, desc = "获取背包")
    public Message getBag(Session session) {
        long roleId = session.getRoleId();
        log.debug("获取背包: roleId={}", roleId);
        // TODO: 实际实现
        return null;
    }
}
