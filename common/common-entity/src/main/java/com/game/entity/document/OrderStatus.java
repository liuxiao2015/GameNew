package com.game.entity.document;

import lombok.Getter;

/**
 * 订单状态
 *
 * @author GameServer
 */
@Getter
public enum OrderStatus {

    PENDING(0, "待支付"),
    PAYING(1, "支付中"),
    PAID(2, "支付成功"),
    DELIVERING(3, "发放中"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    EXPIRED(6, "已过期"),
    FAILED(7, "支付失败"),
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

    public boolean canCancel() {
        return this == PENDING;
    }

    public boolean canPay() {
        return this == PENDING;
    }

    public boolean canRefund() {
        return this == COMPLETED;
    }
}
