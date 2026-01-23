package com.game.service.pay.repository;

import com.game.service.pay.entity.PayOrder;
import com.game.service.pay.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付订单仓库
 *
 * @author GameServer
 */
@Repository
public interface PayOrderRepository extends MongoRepository<PayOrder, String> {
    
    /**
     * 根据订单号查询
     */
    Optional<PayOrder> findByOrderId(String orderId);
    
    /**
     * 根据第三方订单号查询
     */
    Optional<PayOrder> findByTradeNo(String tradeNo);
    
    /**
     * 查询角色的订单
     */
    List<PayOrder> findByRoleIdOrderByCreateTimeDesc(long roleId);
    
    /**
     * 查询角色某状态的订单
     */
    List<PayOrder> findByRoleIdAndStatus(long roleId, OrderStatus status);
    
    /**
     * 查询过期的待支付订单
     */
    List<PayOrder> findByStatusAndExpireTimeBefore(OrderStatus status, LocalDateTime expireTime);
    
    /**
     * 查询需要发放的订单
     */
    List<PayOrder> findByStatusAndDeliverRetryCountLessThan(OrderStatus status, int maxRetry);
    
    /**
     * 统计角色购买商品次数
     */
    long countByRoleIdAndProductIdAndStatus(long roleId, int productId, OrderStatus status);
    
    /**
     * 统计角色某天购买商品次数
     */
    long countByRoleIdAndProductIdAndStatusAndCreateTimeBetween(
            long roleId, int productId, OrderStatus status,
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询角色是否有成功支付记录
     */
    boolean existsByRoleIdAndStatus(long roleId, OrderStatus status);
}
