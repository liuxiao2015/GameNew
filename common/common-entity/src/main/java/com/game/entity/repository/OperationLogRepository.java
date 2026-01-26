package com.game.entity.repository;

import com.game.entity.document.OperationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 操作日志仓库
 *
 * @author GameServer
 */
@Repository
public interface OperationLogRepository extends MongoRepository<OperationLog, String> {

    List<OperationLog> findByRoleIdOrderByOpTimeDesc(long roleId, Pageable pageable);

    List<OperationLog> findByRoleIdAndOpTypeOrderByOpTimeDesc(long roleId, String opType, Pageable pageable);

    List<OperationLog> findByOpTypeAndOpTimeBetween(String opType, long startTime, long endTime);

    List<OperationLog> findByServerIdAndOpTimeBetween(int serverId, long startTime, long endTime);
}
