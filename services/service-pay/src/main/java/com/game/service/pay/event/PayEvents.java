package com.game.service.pay.event;

import com.game.core.event.BaseGameEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 支付相关事件
 *
 * @author GameServer
 */
public class PayEvents {
    
    /**
     * 支付成功事件
     */
    @Getter
    public static class PaySuccessEvent extends BaseGameEvent {
        private final String orderId;
        private final int productId;
        private final BigDecimal amount;
        private final String channel;
        private final boolean firstPay;
        
        public PaySuccessEvent(long roleId, String orderId, int productId, 
                                BigDecimal amount, String channel, boolean firstPay) {
            super(roleId);
            this.orderId = orderId;
            this.productId = productId;
            this.amount = amount;
            this.channel = channel;
            this.firstPay = firstPay;
        }
    }
    
    /**
     * 道具发放完成事件
     */
    @Getter
    public static class DeliverCompleteEvent extends BaseGameEvent {
        private final String orderId;
        private final int productId;
        
        public DeliverCompleteEvent(long roleId, String orderId, int productId) {
            super(roleId);
            this.orderId = orderId;
            this.productId = productId;
        }
    }
    
    /**
     * 首充事件
     */
    @Getter
    public static class FirstPayEvent extends BaseGameEvent {
        private final String orderId;
        private final BigDecimal amount;
        
        public FirstPayEvent(long roleId, String orderId, BigDecimal amount) {
            super(roleId);
            this.orderId = orderId;
            this.amount = amount;
        }
    }
    
    /**
     * 退款事件
     */
    @Getter
    public static class RefundEvent extends BaseGameEvent {
        private final String orderId;
        private final BigDecimal amount;
        private final String reason;
        
        public RefundEvent(long roleId, String orderId, BigDecimal amount, String reason) {
            super(roleId);
            this.orderId = orderId;
            this.amount = amount;
            this.reason = reason;
        }
    }
}
