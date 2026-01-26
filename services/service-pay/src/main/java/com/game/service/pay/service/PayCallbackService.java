package com.game.service.pay.service;

import com.game.api.pay.PayCallbackResult;
import com.game.entity.document.*;
import com.game.entity.repository.*;
import com.game.service.pay.channel.PayChannelAdapter;
import com.game.service.pay.channel.PayChannelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付回调处理服务
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCallbackService {
    
    private final PayOrderRepository orderRepository;
    private final PayRecordRepository recordRepository;
    private final PayChannelFactory channelFactory;
    private final DeliverService deliverService;
    
    /**
     * 处理支付回调
     *
     * @param channel 支付渠道
     * @param params  URL参数
     * @param body    请求body
     * @param headers 请求头
     * @return 响应内容
     */
    @Transactional
    public String handleCallback(PayChannel channel, 
                                  Map<String, String> params, 
                                  String body, 
                                  Map<String, String> headers) {
        log.info("收到支付回调: channel={}", channel);
        
        PayChannelAdapter adapter = channelFactory.getAdapterUnchecked(channel);
        if (adapter == null) {
            log.error("未知的支付渠道: {}", channel);
            return "FAIL";
        }
        
        try {
            // 1. 解析回调
            PayCallbackResult result = adapter.parseCallback(params, body, headers);
            
            if (!result.isSuccess()) {
                log.warn("支付回调解析失败: {}", result.getErrorMsg());
                return adapter.failResponse(result.getErrorMsg());
            }
            
            // 2. 处理支付结果
            boolean success = processPayResult(result, channel);
            
            if (success) {
                return adapter.successResponse();
            } else {
                return adapter.failResponse("处理失败");
            }
            
        } catch (Exception e) {
            log.error("处理支付回调异常: channel={}", channel, e);
            return adapter.failResponse(e.getMessage());
        }
    }
    
    /**
     * 处理支付结果
     */
    private boolean processPayResult(PayCallbackResult result, PayChannel channel) {
        String orderId = result.getOrderId();
        
        // 1. 查询订单
        PayOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            log.error("订单不存在: orderId={}", orderId);
            return false;
        }
        
        // 2. 检查订单状态
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("订单已完成，忽略重复回调: orderId={}", orderId);
            return true;
        }
        
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            log.warn("订单状态异常: orderId={}, status={}", orderId, order.getStatus());
            return false;
        }
        
        // 3. 验证金额
        if (result.getAmount() != null && order.getAmount().compareTo(result.getAmount()) != 0) {
            log.error("订单金额不匹配: orderId={}, expected={}, actual={}", 
                    orderId, order.getAmount(), result.getAmount());
            order.setStatus(OrderStatus.FAILED);
            order.setFailReason("金额不匹配");
            orderRepository.save(order);
            return false;
        }
        
        // 4. 更新订单状态
        order.setTradeNo(result.getTradeNo());
        order.setPayTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);
        order.setCallbackData(result.getRawData());
        orderRepository.save(order);
        
        log.info("订单支付成功: orderId={}, tradeNo={}", orderId, result.getTradeNo());
        
        // 5. 创建支付记录
        createPayRecord(order);
        
        // 6. 发放道具
        deliverService.deliverOrder(order);
        
        return true;
    }
    
    /**
     * 创建支付记录
     */
    private void createPayRecord(PayOrder order) {
        boolean isFirstPay = !recordRepository.existsByRoleId(order.getRoleId());
        long totalSeq = recordRepository.countByRoleId(order.getRoleId()) + 1;
        String today = LocalDate.now().toString();
        long dailySeq = recordRepository.countByRoleIdAndPayDate(order.getRoleId(), today) + 1;
        
        PayRecord record = new PayRecord();
        record.setOrderId(order.getOrderId());
        record.setRoleId(order.getRoleId());
        record.setAccountId(order.getAccountId());
        record.setServerId(order.getServerId());
        record.setProductId(order.getProductId());
        record.setProductName(order.getProductName());
        record.setAmount(order.getAmount());
        record.setCurrency(order.getCurrency());
        record.setChannel(order.getChannel().getCode());
        record.setPayDate(today);
        record.setPayTime(order.getPayTime());
        record.setFirstPay(isFirstPay);
        record.setDailySeq((int) dailySeq);
        record.setTotalSeq((int) totalSeq);
        
        recordRepository.save(record);
        log.info("创建支付记录: orderId={}, isFirstPay={}", order.getOrderId(), isFirstPay);
    }
}
