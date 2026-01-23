package com.game.service.chat.repository;

import com.game.service.chat.entity.MuteInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 禁言信息仓库
 *
 * @author GameServer
 */
public interface MuteInfoRepository extends MongoRepository<MuteInfo, Long> {

    /**
     * 根据角色ID查找禁言信息
     */
    MuteInfo findByRoleId(long roleId);

    /**
     * 根据角色ID删除禁言信息
     */
    void deleteByRoleId(long roleId);
}
