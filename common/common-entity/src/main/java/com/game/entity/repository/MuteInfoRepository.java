package com.game.entity.repository;

import com.game.entity.document.MuteInfo;
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
     * 根据角色ID查找禁言信息
     */
    MuteInfo findByRoleId(long roleId);

    /**
     * 根据角色ID查找禁言信息 (Optional)
     */
    Optional<MuteInfo> findOptionalByRoleId(long roleId);

    /**
     * 根据角色ID删除禁言信息
     */
    void deleteByRoleId(long roleId);

    /**
     * 检查角色是否被禁言
     */
    boolean existsByRoleId(long roleId);
}
