package com.game.service.pay.job;

import com.game.service.pay.entity.PayOrder;
import com.game.service.pay.enums.OrderStatus;
import com.game.service.pay.repository.PayOrderRepository;
import com.game.service.pay.service.DeliverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付订单定时任务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayOrderJob {
    
    private final PayOrderRepository orderRepository;
    private final DeliverService deliverService;
    
    /**
     * 最大发放重试次数
     */
    private static final int MAX_DELIVER_RETRY = 3;
    
    /**
     * 过期未支付订单处理
     * 每5分钟执行
     */
    @Scheduled(fixedRate = 300000)
    public void expireOrders() {
        List<PayOrder> expiredOrders = orderRepository.findByStatusAndExpireTimeBefore(
                OrderStatus.PENDING, LocalDateTime.now());
        
        if (expiredOrders.isEmpty()) {
            return;
        }
        
        log.info("处理过期订单: count={}", expiredOrders.size());
        
        for (PayOrder order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            log.info("订单已过期: orderId={}", order.getOrderId());
        }
    }
    
    /**
     * 支付中订单超时处理
     * 每5分钟执行
     */
    @Scheduled(fixedRate = 300000)
    public void timeoutPayingOrders() {
        // 支付中超过30分钟的订单
        List<PayOrder> payingOrders = orderRepository.findByStatusAndExpireTimeBefore(
                OrderStatus.PAYING, LocalDateTime.now());
        
        if (payingOrders.isEmpty()) {
            return;
        }
        
        log.info("处理支付超时订单: count={}", payingOrders.size());
        
        for (PayOrder order : payingOrders) {
            // TODO: 可以调用渠道查询接口确认订单状态
            order.setStatus(OrderStatus.EXPIRED);
            order.setRemark("支付超时");
            orderRepository.save(order);
            log.info("支付超时订单: orderId={}", order.getOrderId());
        }
    }
    
    /**
     * 发放失败订单重试
     * 每1分钟执行
     */
    @Scheduled(fixedRate = 60000)
    public void retryDeliverOrders() {
        // 查询已支付但未发放完成的订单 (重试次数未超限)
        List<PayOrder> paidOrders = orderRepository.findByStatusAndDeliverRetryCountLessThan(
                OrderStatus.PAID, MAX_DELIVER_RETRY);
        
        if (paidOrders.isEmpty()) {
            return;
        }
        
        log.info("重试发放订单: count={}", paidOrders.size());
        
        for (PayOrder order : paidOrders) {
            try {
                deliverService.deliverOrder(order);
            } catch (Exception e) {
                log.error("重试发放订单失败: orderId={}", order.getOrderId(), e);
            }
        }
    }
}
