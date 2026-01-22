package com.game.entity.chat;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 禁言信息仓库
 *
 * @author GameServer
 */
@Repository
public interface MuteInfoRepository extends MongoRepository<MuteInfo, Long> {

    /**
     * 根据角色 ID 查询
     */
    Optional<MuteInfo> findByRoleId(long roleId);

    /**
     * 删除指定角色的禁言
     */
    void deleteByRoleId(long roleId);

    /**
     * 检查角色是否被禁言
     */
    boolean existsByRoleId(long roleId);
}
