package com.game.common.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * 推送消息基类
 * <p>
 * 所有服务端主动推送的消息都应继承此类
 * </p>
 *
 * @author GameServer
 */
@Data
public abstract class BasePush implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 推送类型
     */
    private int pushType;

    /**
     * 协议号
     */
    private int protocolId;

    /**
     * 服务器时间戳
     */
    private long serverTime;

    public BasePush() {
        this.serverTime = System.currentTimeMillis();
    }
}
