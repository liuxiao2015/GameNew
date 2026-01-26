package com.game.entity.repository;

import com.game.entity.document.ActivityConfig;
import com.game.entity.document.ActivityStatus;
import com.game.entity.document.ActivityType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活动配置仓库
 *
 * @author GameServer
 */
@Repository
public interface ActivityConfigRepository extends MongoRepository<ActivityConfig, String> {

    Optional<ActivityConfig> findByActivityId(int activityId);

    List<ActivityConfig> findByStatus(ActivityStatus status);

    List<ActivityConfig> findByType(ActivityType type);

    List<ActivityConfig> findByTypeAndStatus(ActivityType type, ActivityStatus status);

    List<ActivityConfig> findByStartTimeBeforeAndEndTimeAfter(LocalDateTime now, LocalDateTime now2);

    List<ActivityConfig> findByStatusAndStartTimeBetween(ActivityStatus status,
                                                          LocalDateTime start,
                                                          LocalDateTime end);

    List<ActivityConfig> findByStatusAndEndTimeBefore(ActivityStatus status, LocalDateTime time);

    List<ActivityConfig> findByStatusInOrderBySortOrderAsc(List<ActivityStatus> statuses);
}
