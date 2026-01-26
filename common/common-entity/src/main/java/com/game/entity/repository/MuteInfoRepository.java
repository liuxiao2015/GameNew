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

    Optional<MuteInfo> findByRoleId(long roleId);

    void deleteByRoleId(long roleId);

    boolean existsByRoleId(long roleId);
}
