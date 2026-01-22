package com.game.entity.log;

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

    /**
     * 根据角色 ID 查询
     */
    List<OperationLog> findByRoleIdOrderByOpTimeDesc(long roleId, Pageable pageable);

    /**
     * 根据角色 ID 和操作类型查询
     */
    List<OperationLog> findByRoleIdAndOpTypeOrderByOpTimeDesc(long roleId, String opType, Pageable pageable);

    /**
     * 根据操作类型和时间范围查询
     */
    List<OperationLog> findByOpTypeAndOpTimeBetween(String opType, long startTime, long endTime);

    /**
     * 根据服务器 ID 和时间范围查询
     */
    List<OperationLog> findByServerIdAndOpTimeBetween(int serverId, long startTime, long endTime);
}
