package com.game.service.pay.channel;

import com.game.api.pay.OrderDTO;
import com.game.api.pay.PayCallbackResult;
import com.game.entity.document.PayChannel;
import com.game.entity.document.PayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 模拟支付适配器 (测试用)
 *
 * @author GameServer
 */
@Slf4j
@Component
public class MockPayAdapter extends AbstractPayChannelAdapter {
    
    @Value("${spring.profiles.active:dev}")
    private String profile;
    
    @Override
    public PayChannel getChannel() {
        return PayChannel.MOCK;
    }
    
    @Override
    public boolean isEnabled() {
        // 仅在开发环境启用
        return "dev".equals(profile) || "test".equals(profile);
    }
    
    @Override
    public OrderDTO.PayParams createPayRequest(PayOrder order) {
        log.info("[模拟支付] 创建支付请求: orderId={}, amount={}", 
                order.getOrderId(), order.getAmount());
        
        return OrderDTO.PayParams.builder()
                .prepayId("mock_" + order.getOrderId())
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .nonceStr(UUID.randomUUID().toString())
                .sign("mock_sign")
                .extra("MOCK_PAY")
                .build();
    }
    
    @Override
    public PayCallbackResult parseCallback(Map<String, String> params, String body, Map<String, String> headers) {
        String orderId = params.get("orderId");
        String tradeNo = params.get("tradeNo");
        String amountStr = params.get("amount");
        
        if (orderId == null) {
            return PayCallbackResult.fail("缺少orderId参数");
        }
        
        log.info("[模拟支付] 收到支付回调: orderId={}, tradeNo={}, amount={}", 
                orderId, tradeNo, amountStr);
        
        return PayCallbackResult.builder()
                .success(true)
                .orderId(orderId)
                .tradeNo(tradeNo != null ? tradeNo : "mock_trade_" + System.currentTimeMillis())
                .amount(amountStr != null ? new java.math.BigDecimal(amountStr) : null)
                .rawData(body)
                .build();
    }
    
    @Override
    public boolean verifySignature(Map<String, String> params, String body) {
        // 模拟支付不验签
        return true;
    }
    
    @Override
    protected String generateSign(Map<String, String> params) {
        return "mock_sign";
    }
    
    @Override
    public PayCallbackResult queryOrder(PayOrder order) {
        log.info("[模拟支付] 查询订单: orderId={}", order.getOrderId());
        return PayCallbackResult.builder()
                .success(true)
                .orderId(order.getOrderId())
                .tradeNo(order.getTradeNo())
                .amount(order.getAmount())
                .build();
    }
    
    @Override
    public boolean refund(PayOrder order, String reason) {
        log.info("[模拟支付] 退款: orderId={}, reason={}", order.getOrderId(), reason);
        return true;
    }
    
    @Override
    public String successResponse() {
        return "{\"code\":0,\"msg\":\"success\"}";
    }
    
    @Override
    public String failResponse(String message) {
        return "{\"code\":-1,\"msg\":\"" + message + "\"}";
    }
}
