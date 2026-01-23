package com.game.service.rank.repository;

import com.game.service.rank.entity.RankSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 排行榜快照仓库
 *
 * @author GameServer
 */
@Repository
public interface RankSnapshotRepository extends MongoRepository<RankSnapshot, String> {

    /**
     * 根据排行榜类型和日期查找快照
     */
    List<RankSnapshot> findByRankTypeAndDate(int rankType, String date);

    /**
     * 根据快照名称查找
     */
    RankSnapshot findBySnapshotName(String snapshotName);

    /**
     * 删除指定日期之前的快照
     */
    void deleteByDateBefore(String date);
}
