package com.game.service.pay.enums;

import lombok.Getter;

/**
 * 支付渠道
 *
 * @author GameServer
 */
@Getter
public enum PayChannel {
    
    /**
     * 微信支付
     */
    WECHAT("wechat", "微信支付"),
    
    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),
    
    /**
     * 苹果内购
     */
    APPLE_IAP("apple", "苹果内购"),
    
    /**
     * Google Play
     */
    GOOGLE_PLAY("google", "Google Play"),
    
    /**
     * 模拟支付 (测试用)
     */
    MOCK("mock", "模拟支付");
    
    private final String code;
    private final String name;
    
    PayChannel(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public static PayChannel of(String code) {
        if (code == null) {
            return null;
        }
        for (PayChannel channel : values()) {
            if (channel.code.equalsIgnoreCase(code)) {
                return channel;
            }
        }
        return null;
    }
}
