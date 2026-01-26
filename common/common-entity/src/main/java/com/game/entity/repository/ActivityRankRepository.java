package com.game.entity.repository;

import com.game.entity.document.ActivityRank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 活动排行仓库
 *
 * @author GameServer
 */
@Repository
public interface ActivityRankRepository extends MongoRepository<ActivityRank, String> {

    Optional<ActivityRank> findByActivityIdAndActivityVersionAndRoleId(int activityId, int activityVersion, long roleId);

    List<ActivityRank> findByActivityIdAndActivityVersionOrderByScoreDesc(int activityId, int activityVersion, Pageable pageable);

    List<ActivityRank> findByActivityIdAndActivityVersionOrderByScoreDesc(int activityId, int activityVersion);

    void deleteByActivityIdAndActivityVersion(int activityId, int activityVersion);

    long countByActivityIdAndActivityVersion(int activityId, int activityVersion);
}
