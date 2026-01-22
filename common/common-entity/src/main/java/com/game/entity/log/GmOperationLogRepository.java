package com.game.entity.log;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * GM 操作日志仓库
 *
 * @author GameServer
 */
@Repository
public interface GmOperationLogRepository extends MongoRepository<GmOperationLog, String> {

    /**
     * 根据操作人查询
     */
    List<GmOperationLog> findByOperatorOrderByOpTimeDesc(String operator, Pageable pageable);

    /**
     * 根据操作类型查询
     */
    List<GmOperationLog> findByOpTypeOrderByOpTimeDesc(String opType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    List<GmOperationLog> findByOpTimeBetweenOrderByOpTimeDesc(long startTime, long endTime, Pageable pageable);

    /**
     * 根据目标 ID 查询
     */
    List<GmOperationLog> findByTargetIdOrderByOpTimeDesc(String targetId, Pageable pageable);
}
