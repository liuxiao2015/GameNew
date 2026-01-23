package com.game.api.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付回调结果
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayCallbackResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 业务订单号
     */
    private String orderId;
    
    /**
     * 第三方订单号
     */
    private String tradeNo;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 原始回调数据
     */
    private String rawData;
    
    public static PayCallbackResult success(String orderId, String tradeNo, BigDecimal amount) {
        return PayCallbackResult.builder()
                .success(true)
                .orderId(orderId)
                .tradeNo(tradeNo)
                .amount(amount)
                .build();
    }
    
    public static PayCallbackResult fail(String errorMsg) {
        return PayCallbackResult.builder()
                .success(false)
                .errorMsg(errorMsg)
                .build();
    }
}
