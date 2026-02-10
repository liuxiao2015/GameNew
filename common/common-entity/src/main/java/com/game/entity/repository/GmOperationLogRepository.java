package com.game.entity.repository;

import com.game.entity.document.GmOperationLog;
import org.springframework.data.domain.Page;
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
     * 根据操作者查询
     */
    Page<GmOperationLog> findByOperatorOrderByOperateTimeDesc(String operator, Pageable pageable);

    /**
     * 根据操作类型查询
     */
    Page<GmOperationLog> findByOperationTypeOrderByOperateTimeDesc(String operationType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    List<GmOperationLog> findByOperateTimeBetweenOrderByOperateTimeDesc(long startTime, long endTime);

    /**
     * 根据目标角色查询
     */
    List<GmOperationLog> findByTargetRoleIdOrderByOperateTimeDesc(long targetRoleId);
}
