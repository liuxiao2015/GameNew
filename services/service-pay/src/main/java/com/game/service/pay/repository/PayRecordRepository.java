package com.game.service.pay.repository;

import com.game.service.pay.entity.PayRecord;
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
    
    /**
     * 根据订单号查询
     */
    Optional<PayRecord> findByOrderId(String orderId);
    
    /**
     * 查询角色的支付记录
     */
    List<PayRecord> findByRoleIdOrderByPayTimeDesc(long roleId);
    
    /**
     * 查询角色某商品的购买记录
     */
    List<PayRecord> findByRoleIdAndProductId(long roleId, int productId);
    
    /**
     * 统计角色购买商品次数
     */
    long countByRoleIdAndProductId(long roleId, int productId);
    
    /**
     * 统计角色某天购买商品次数
     */
    long countByRoleIdAndProductIdAndPayDate(long roleId, int productId, String payDate);
    
    /**
     * 检查是否首充
     */
    boolean existsByRoleId(long roleId);
    
    /**
     * 统计角色支付次数
     */
    long countByRoleId(long roleId);
    
    /**
     * 统计角色某天支付次数
     */
    long countByRoleIdAndPayDate(long roleId, String payDate);
}
