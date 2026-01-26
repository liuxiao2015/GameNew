package com.game.entity.repository;

import com.game.entity.document.OrderStatus;
import com.game.entity.document.PayOrder;
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

    Optional<PayOrder> findByOrderId(String orderId);

    Optional<PayOrder> findByTradeNo(String tradeNo);

    List<PayOrder> findByRoleIdOrderByCreateTimeDesc(long roleId);

    List<PayOrder> findByRoleIdAndStatus(long roleId, OrderStatus status);

    List<PayOrder> findByStatusAndExpireTimeBefore(OrderStatus status, LocalDateTime expireTime);

    List<PayOrder> findByStatusAndDeliverRetryCountLessThan(OrderStatus status, int maxRetry);

    long countByRoleIdAndProductIdAndStatus(long roleId, int productId, OrderStatus status);

    long countByRoleIdAndProductIdAndStatusAndCreateTimeBetween(
            long roleId, int productId, OrderStatus status,
            LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByRoleIdAndStatus(long roleId, OrderStatus status);
}
