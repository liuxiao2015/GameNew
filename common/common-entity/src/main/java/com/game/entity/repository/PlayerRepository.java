package com.game.entity.repository;

import com.game.entity.document.PlayerData;
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

    Optional<PlayerData> findByRoleId(long roleId);

    Optional<PlayerData> findByAccountIdAndServerId(long accountId, int serverId);

    Optional<PlayerData> findByRoleName(String roleName);

    List<PlayerData> findByGuildId(long guildId);

    List<PlayerData> findByAccountId(long accountId);

    boolean existsByRoleName(String roleName);

    List<PlayerData> findByLevelBetween(int minLevel, int maxLevel);

    List<PlayerData> findTop100ByOrderByCombatPowerDesc();
}
