package com.game.service.pay.service;

import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.service.pay.entity.PayOrder;
import com.game.service.pay.entity.Product;
import com.game.service.pay.enums.OrderStatus;
import com.game.service.pay.repository.PayOrderRepository;
import com.game.service.pay.repository.PayRecordRepository;
import com.game.service.pay.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 道具发放服务
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverService {
    
    private final PayOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PayRecordRepository recordRepository;
    
    @DubboReference(check = false, timeout = 10000, retries = 2)
    private PlayerService playerService;
    
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY = 3;
    
    /**
     * 发放订单道具
     */
    @Async
    public void deliverOrder(PayOrder order) {
        log.info("开始发放订单道具: orderId={}, roleId={}", order.getOrderId(), order.getRoleId());
        
        try {
            // 更新状态为发放中
            order.setStatus(OrderStatus.DELIVERING);
            order.setDeliverTime(LocalDateTime.now());
            orderRepository.save(order);
            
            // 获取商品信息
            Product product = productRepository.findByProductId(order.getProductId()).orElse(null);
            if (product == null) {
                throw new RuntimeException("商品不存在: " + order.getProductId());
            }
            
            // 检查是否首充
            boolean isFirstPay = !recordRepository.existsByRoleId(order.getRoleId());
            
            // 发放道具
            List<DeliverItem> items = new ArrayList<>();
            
            // 基础奖励
            if (product.getRewards() != null) {
                for (Product.ProductReward reward : product.getRewards()) {
                    items.add(new DeliverItem(reward.getType(), reward.getItemId(), reward.getCount()));
                }
            }
            
            // 首充额外奖励
            if (isFirstPay && product.getFirstBuyRewards() != null) {
                for (Product.ProductReward reward : product.getFirstBuyRewards()) {
                    items.add(new DeliverItem(reward.getType(), reward.getItemId(), reward.getCount()));
                }
                log.info("首充额外奖励: orderId={}", order.getOrderId());
            }
            
            // 执行发放
            boolean success = doDeliver(order.getRoleId(), items, order.getOrderId());
            
            if (success) {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompleteTime(LocalDateTime.now());
                log.info("订单道具发放完成: orderId={}", order.getOrderId());
            } else {
                throw new RuntimeException("道具发放失败");
            }
            
        } catch (Exception e) {
            log.error("订单道具发放失败: orderId={}", order.getOrderId(), e);
            
            order.setDeliverRetryCount(order.getDeliverRetryCount() + 1);
            
            if (order.getDeliverRetryCount() >= MAX_RETRY) {
                order.setStatus(OrderStatus.FAILED);
                order.setFailReason("发放失败: " + e.getMessage());
                log.error("订单发放超过最大重试次数: orderId={}", order.getOrderId());
            } else {
                order.setStatus(OrderStatus.PAID); // 回退状态，等待重试
            }
        }
        
        orderRepository.save(order);
    }
    
    /**
     * 执行道具发放
     */
    private boolean doDeliver(long roleId, List<DeliverItem> items, String orderId) {
        String reason = "充值奖励-订单:" + orderId;
        
        for (DeliverItem item : items) {
            Result<Void> result;
            
            switch (item.type) {
                case 1: // 货币
                    result = deliverCurrency(roleId, item.itemId, item.count, reason);
                    break;
                case 2: // 道具
                    result = playerService.addItem(roleId, item.itemId, item.count, reason);
                    break;
                case 3: // VIP经验
                    // TODO: 实现VIP经验发放
                    result = Result.success();
                    break;
                default:
                    log.warn("未知的道具类型: type={}", item.type);
                    result = Result.success();
            }
            
            if (result == null || !result.isSuccess()) {
                log.error("道具发放失败: roleId={}, item={}, result={}", roleId, item, result);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 发放货币
     */
    private Result<Void> deliverCurrency(long roleId, int currencyType, long count, String reason) {
        return switch (currencyType) {
            case 1 -> // 金币
                    playerService.addGold(roleId, count, reason);
            case 2 -> // 钻石
                    playerService.addDiamond(roleId, count, reason);
            default -> {
                log.warn("未知的货币类型: currencyType={}", currencyType);
                yield Result.success();
            }
        };
    }
    
    /**
     * 发放道具项
     */
    private record DeliverItem(int type, int itemId, long count) {
    }
}
