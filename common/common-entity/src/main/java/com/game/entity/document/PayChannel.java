package com.game.entity.document;

import lombok.Getter;

/**
 * 支付渠道
 *
 * @author GameServer
 */
@Getter
public enum PayChannel {

    WECHAT("wechat", "微信支付"),
    ALIPAY("alipay", "支付宝"),
    APPLE_IAP("apple", "苹果内购"),
    GOOGLE_PLAY("google", "Google Play"),
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
