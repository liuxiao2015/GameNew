package com.game.service.pay.enums;

import lombok.Getter;

/**
 * 订单状态
 *
 * @author GameServer
 */
@Getter
public enum OrderStatus {
    
    /**
     * 待支付
     */
    PENDING(0, "待支付"),
    
    /**
     * 支付中 (已发起支付请求)
     */
    PAYING(1, "支付中"),
    
    /**
     * 支付成功
     */
    PAID(2, "支付成功"),
    
    /**
     * 发放中 (道具发放中)
     */
    DELIVERING(3, "发放中"),
    
    /**
     * 已完成 (道具已发放)
     */
    COMPLETED(4, "已完成"),
    
    /**
     * 已取消
     */
    CANCELLED(5, "已取消"),
    
    /**
     * 已过期
     */
    EXPIRED(6, "已过期"),
    
    /**
     * 支付失败
     */
    FAILED(7, "支付失败"),
    
    /**
     * 已退款
     */
    REFUNDED(8, "已退款");
    
    private final int code;
    private final String desc;
    
    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING;
    }
    
    /**
     * 是否可以支付
     */
    public boolean canPay() {
        return this == PENDING;
    }
    
    /**
     * 是否可以退款
     */
    public boolean canRefund() {
        return this == COMPLETED;
    }
}
