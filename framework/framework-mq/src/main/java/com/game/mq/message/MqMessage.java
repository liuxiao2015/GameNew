package com.game.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息队列消息基类
 * <p>
 * 所有 MQ 消息都应继承此类
 * </p>
 *
 * @author GameServer
 */
@Data
public abstract class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 发送时间戳
     */
    private long timestamp;

    /**
     * 来源服务
     */
    private String sourceService;

    /**
     * 扩展属性
     */
    private Map<String, String> properties;

    protected MqMessage() {
        this.messageId = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = System.currentTimeMillis();
        this.messageType = this.getClass().getSimpleName();
        this.properties = new HashMap<>();
    }

    /**
     * 添加属性
     */
    public MqMessage addProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
        return this;
    }

    /**
     * 获取属性
     */
    public String getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
}
