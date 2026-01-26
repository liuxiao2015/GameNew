package com.game.entity.repository;

import com.game.entity.document.PlayerActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家活动数据仓库
 *
 * @author GameServer
 */
@Repository
public interface PlayerActivityRepository extends MongoRepository<PlayerActivity, String> {

    Optional<PlayerActivity> findByRoleIdAndActivityId(long roleId, int activityId);

    Optional<PlayerActivity> findByRoleIdAndActivityIdAndActivityVersion(long roleId, int activityId, int activityVersion);

    List<PlayerActivity> findByRoleId(long roleId);

    List<PlayerActivity> findByActivityId(int activityId);

    List<PlayerActivity> findByActivityIdAndActivityVersion(int activityId, int activityVersion);

    void deleteByActivityId(int activityId);

    void deleteByActivityIdAndActivityVersion(int activityId, int activityVersion);
}
