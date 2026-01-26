package com.game.entity.repository;

import com.game.entity.document.PayRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付记录仓库
 *
 * @author GameServer
 */
@Repository
public interface PayRecordRepository extends MongoRepository<PayRecord, String> {

    Optional<PayRecord> findByOrderId(String orderId);

    List<PayRecord> findByRoleIdOrderByPayTimeDesc(long roleId);

    List<PayRecord> findByRoleIdAndProductId(long roleId, int productId);

    long countByRoleIdAndProductId(long roleId, int productId);

    long countByRoleIdAndProductIdAndPayDate(long roleId, int productId, String payDate);

    boolean existsByRoleId(long roleId);

    long countByRoleId(long roleId);

    long countByRoleIdAndPayDate(long roleId, String payDate);
}
