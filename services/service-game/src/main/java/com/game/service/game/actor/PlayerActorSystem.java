package com.game.service.game.actor;

import com.game.actor.core.ActorSystem;
import com.game.service.game.repository.PlayerRepository;
import org.springframework.stereotype.Component;

/**
 * 玩家 Actor 系统
 * <p>
 * 管理所有在线玩家的 Actor 实例
 * </p>
 *
 * @author GameServer
 */
@Component
public class PlayerActorSystem extends ActorSystem<PlayerActor> {

    public PlayerActorSystem(PlayerRepository playerRepository) {
        super(
            "PlayerActorSystem",
            ActorSystemConfig.create()
                .maxSize(50000)
                .idleTimeoutMinutes(30)
                .saveIntervalSeconds(60),
            roleId -> new PlayerActor(roleId, playerRepository)
        );
    }

    /**
     * 玩家进入游戏
     */
    public PlayerActor enterGame(long roleId, String sessionId) {
        PlayerActor actor = getActor(roleId);
        if (actor != null) {
            actor.fire("LOGIN", new PlayerActor.LoginData(sessionId));
        }
        return actor;
    }

    /**
     * 玩家离开游戏
     */
    public void leaveGame(long roleId) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("LOGOUT");
            removeActor(roleId);
        }
    }

    /**
     * 增加金币
     */
    public void addGold(long roleId, long amount, String reason) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("ADD_GOLD", new PlayerActor.AddGoldData(amount, reason));
        }
    }

    /**
     * 增加经验
     */
    public void addExp(long roleId, long amount, String reason) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("ADD_EXP", new PlayerActor.AddExpData(amount, reason));
        }
    }

    /**
     * 设置公会信息
     */
    public void setGuild(long roleId, long guildId, String guildName, int position) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("SET_GUILD", new PlayerActor.SetGuildData(guildId, guildName, position));
        }
    }
}
