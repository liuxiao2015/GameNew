package com.game.entity.repository;

import com.game.entity.document.GmOperationLog;
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

    List<GmOperationLog> findByOperatorOrderByOpTimeDesc(String operator, Pageable pageable);

    List<GmOperationLog> findByOpTypeOrderByOpTimeDesc(String opType, Pageable pageable);

    List<GmOperationLog> findByOpTimeBetweenOrderByOpTimeDesc(long startTime, long endTime, Pageable pageable);

    List<GmOperationLog> findByTargetIdOrderByOpTimeDesc(String targetId, Pageable pageable);
}
