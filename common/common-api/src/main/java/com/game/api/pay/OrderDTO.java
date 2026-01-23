package com.game.api.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单号
     */
    private String orderId;
    
    /**
     * 第三方订单号
     */
    private String tradeNo;
    
    /**
     * 角色ID
     */
    private long roleId;
    
    /**
     * 商品ID
     */
    private int productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 货币类型
     */
    private String currency;
    
    /**
     * 支付渠道
     */
    private String channel;
    
    /**
     * 订单状态码
     */
    private int status;
    
    /**
     * 订单状态描述
     */
    private String statusDesc;
    
    /**
     * 创建时间
     */
    private long createTime;
    
    /**
     * 支付时间
     */
    private long payTime;
    
    /**
     * 过期时间
     */
    private long expireTime;
    
    /**
     * 支付参数 (用于客户端唤起支付)
     */
    private PayParams payParams;
    
    /**
     * 支付参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayParams implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 微信 prepay_id
         */
        private String prepayId;
        
        /**
         * 支付宝 order_string
         */
        private String orderString;
        
        /**
         * 签名
         */
        private String sign;
        
        /**
         * 时间戳
         */
        private String timestamp;
        
        /**
         * 随机字符串
         */
        private String nonceStr;
        
        /**
         * 扩展参数
         */
        private String extra;
    }
}
