package com.game.service.game.repository;

import com.game.entity.player.PlayerData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 玩家数据仓库
 *
 * @author GameServer
 */
@Repository
public interface PlayerRepository extends MongoRepository<PlayerData, Long> {

    /**
     * 根据角色 ID 查询
     */
    Optional<PlayerData> findByRoleId(long roleId);

    /**
     * 根据账号 ID 和服务器 ID 查询
     */
    Optional<PlayerData> findByAccountIdAndServerId(long accountId, int serverId);

    /**
     * 判断角色名是否存在
     */
    boolean existsByRoleName(String roleName);
}
