package com.game.service.game.actor;

import com.game.actor.core.ActorSystem;
import com.game.config.container.LevelConfigContainer;
import com.game.core.event.EventBus;
import com.game.entity.repository.PlayerRepository;
import org.springframework.stereotype.Component;

/**
 * 玩家 Actor 系统
 * <p>
 * 管理所有在线玩家的 Actor 实例
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - ActorSystem: 高性能 Actor 容器管理
 * - EventBus: 注入到每个 Actor 用于事件发布
 * - ConfigContainer: 注入到每个 Actor 用于配置读取
 * </pre>
 *
 * @author GameServer
 */
@Component
public class PlayerActorSystem extends ActorSystem<PlayerActor> {

    private final EventBus eventBus;
    private final LevelConfigContainer levelConfigContainer;

    public PlayerActorSystem(PlayerRepository playerRepository, 
                             EventBus eventBus,
                             LevelConfigContainer levelConfigContainer) {
        super(
            "PlayerActorSystem",
            ActorSystemConfig.create()
                .maxSize(50000)
                .idleTimeoutMinutes(30)
                .saveIntervalSeconds(60),
            roleId -> {
                PlayerActor actor = new PlayerActor(roleId, playerRepository);
                return actor;
            }
        );
        this.eventBus = eventBus;
        this.levelConfigContainer = levelConfigContainer;
    }

    /**
     * 获取或创建 Actor，并注入依赖
     */
    @Override
    public PlayerActor getActor(long actorId) {
        PlayerActor actor = super.getActor(actorId);
        if (actor != null) {
            // 注入依赖
            actor.setEventBus(eventBus);
            actor.setLevelConfigContainer(levelConfigContainer);
        }
        return actor;
    }

    /**
     * 玩家进入游戏
     */
    public PlayerActor enterGame(long roleId, String sessionId, int serverId) {
        PlayerActor actor = getActor(roleId);
        if (actor != null) {
            actor.fire("LOGIN", new PlayerActor.LoginData(sessionId, serverId));
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
     * 增加钻石
     */
    public void addDiamond(long roleId, long amount, String reason) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("ADD_DIAMOND", new PlayerActor.AddDiamondData(amount, reason));
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
     * 添加物品
     */
    public void addItem(long roleId, int itemId, long count, String reason) {
        PlayerActor actor = getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("ADD_ITEM", new PlayerActor.AddItemData(itemId, count, reason));
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
