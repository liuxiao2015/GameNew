package com.game.common.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求基类
 * <p>
 * 所有客户端请求消息都应继承此类
 * </p>
 *
 * @author GameServer
 */
@Data
public abstract class BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求序号 (用于请求-响应配对)
     */
    private int seqId;

    /**
     * 协议号
     */
    private int protocolId;

    /**
     * 方法号
     */
    private int methodId;

    /**
     * 请求时间戳
     */
    private long timestamp;

    /**
     * 客户端版本
     */
    private String clientVersion;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 获取协议标识 (协议号 + 方法号)
     */
    public int getProtocolKey() {
        return (protocolId << 16) | methodId;
    }
}
