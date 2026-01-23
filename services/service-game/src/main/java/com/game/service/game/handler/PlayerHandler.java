package com.game.service.game.handler;

import com.game.api.common.MethodId;
import com.game.api.common.ProtocolConstants;
import com.game.common.enums.ErrorCode;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.entity.player.PlayerData;
import com.game.proto.*;
import com.game.service.game.actor.PlayerActor;
import com.game.service.game.actor.PlayerActorSystem;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家协议处理器
 * <p>
 * 处理所有与玩家相关的协议请求
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_PLAYER, value = "玩家模块")
@RequiredArgsConstructor
public class PlayerHandler extends BaseHandler {

    private final PlayerActorSystem playerActorSystem;

    /**
     * 获取玩家信息
     */
    @Protocol(methodId = MethodId.Player.GET_INFO, desc = "获取玩家信息", requireLogin = true, requireRole = true)
    public Message getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
        long roleId = request.getRoleId() > 0 ? request.getRoleId() : session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        return S2C_GetPlayerInfo.newBuilder()
                .setResult(buildSuccessResult())
                .setPlayer(buildPlayerInfo(actor.getData()))
                .build();
    }

    /**
     * 更新玩家信息 (头像、签名等)
     */
    @Protocol(methodId = MethodId.Player.UPDATE_INFO, desc = "更新玩家信息", requireLogin = true, requireRole = true)
    public Message updatePlayerInfo(Session session, C2S_UpdatePlayer request) {
        long roleId = session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        // 通过 Actor 消息更新
        actor.fire("UPDATE_INFO", new UpdateInfoData(
                request.getAvatarId(),
                request.getFrameId(),
                request.getSignature()
        ));

        return S2C_UpdatePlayer.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 修改名字
     */
    @Protocol(methodId = MethodId.Player.CHANGE_NAME, desc = "修改名字", requireLogin = true, requireRole = true)
    public Message changeName(Session session, C2S_ChangeName request) {
        long roleId = session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 检查名字合法性和重复性
        String newName = request.getNewName();

        // 通过 Actor 消息更新
        actor.fire("CHANGE_NAME", newName);

        return S2C_ChangeName.newBuilder()
                .setResult(buildSuccessResult())
                .setNewName(newName)
                .build();
    }

    /**
     * 获取背包
     */
    @Protocol(methodId = MethodId.Player.GET_BAG, desc = "获取背包", requireLogin = true, requireRole = true)
    public Message getBag(Session session, C2S_GetBag request) {
        long roleId = session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 从玩家数据获取背包物品
        return S2C_GetBag.newBuilder()
                .setResult(buildSuccessResult())
                .setCapacity(100)
                .build();
    }

    /**
     * 使用物品
     */
    @Protocol(methodId = MethodId.Player.USE_ITEM, desc = "使用物品", requireLogin = true, requireRole = true)
    public Message useItem(Session session, C2S_UseItem request) {
        long roleId = session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 实现使用物品逻辑
        return S2C_UseItem.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 出售物品
     */
    @Protocol(methodId = MethodId.Player.SELL_ITEM, desc = "出售物品", requireLogin = true, requireRole = true)
    public Message sellItem(Session session, C2S_SellItem request) {
        long roleId = session.getRoleId();
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            throwError(ErrorCode.ROLE_NOT_FOUND);
        }

        // TODO: 实现出售物品逻辑
        return S2C_SellItem.newBuilder()
                .setResult(buildSuccessResult())
                .setGold(0)
                .build();
    }

    // ==================== 辅助方法 ====================

    private PlayerInfo buildPlayerInfo(PlayerData data) {
        return PlayerInfo.newBuilder()
                .setRoleId(data.getRoleId())
                .setRoleName(data.getRoleName())
                .setLevel(data.getLevel())
                .setExp(data.getExp())
                .setGold(data.getGold())
                .setDiamond(data.getDiamond())
                .setBindDiamond(data.getBindDiamond())
                .setVipLevel(data.getVipLevel())
                .setVipExp(data.getVipExp())
                .setAvatarId(data.getAvatarId())
                .setFrameId(data.getFrameId())
                .setEnergy(data.getEnergy())
                .setMaxEnergy(data.getMaxEnergy())
                .setEnergyRecoverTime(data.getEnergyRecoverTime())
                .setCombatPower(data.getCombatPower())
                .setGuildId(data.getGuildId())
                .setGuildName(data.getGuildName() != null ? data.getGuildName() : "")
                .setSignature(data.getSignature() != null ? data.getSignature() : "")
                .setCreateTime(data.getCreateTime() != null ? 
                    data.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                .setLastLoginTime(data.getLastLoginTime())
                .build();
    }

    private com.game.proto.Result buildSuccessResult() {
        return com.game.proto.Result.newBuilder()
                .setCode(0)
                .setMessage("success")
                .build();
    }

    /**
     * 抛出业务异常
     * <p>
     * 不再返回错误消息，而是抛出 BizException，由 ProtocolDispatcher 统一处理
     * </p>
     */
    private void throwError(ErrorCode errorCode) {
        throw new com.game.common.exception.BizException(errorCode);
    }

    // ==================== 消息数据类 ====================

    public record UpdateInfoData(int avatarId, int frameId, String signature) {}
}
