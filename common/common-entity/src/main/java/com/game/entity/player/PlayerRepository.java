package com.game.entity.player;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家数据仓库
 *
 * @author GameServer
 */
@Repository
public interface PlayerRepository extends MongoRepository<PlayerData, String> {

    /**
     * 根据角色 ID 查询
     */
    Optional<PlayerData> findByRoleId(long roleId);

    /**
     * 根据账号 ID 和服务器 ID 查询
     */
    Optional<PlayerData> findByAccountIdAndServerId(long accountId, int serverId);

    /**
     * 根据角色名查询
     */
    Optional<PlayerData> findByRoleName(String roleName);

    /**
     * 根据公会 ID 查询成员
     */
    List<PlayerData> findByGuildId(long guildId);

    /**
     * 根据账号 ID 查询所有角色
     */
    List<PlayerData> findByAccountId(long accountId);

    /**
     * 检查角色名是否存在
     */
    boolean existsByRoleName(String roleName);

    /**
     * 根据等级范围查询
     */
    List<PlayerData> findByLevelBetween(int minLevel, int maxLevel);

    /**
     * 查询战力排行
     */
    List<PlayerData> findTop100ByOrderByCombatPowerDesc();
}
