package com.game.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 事件消息
 * <p>
 * 用于分布式事件广播
 * </p>
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EventMessage extends MqMessage {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型名
     */
    private String eventType;

    /**
     * 事件数据 (JSON)
     */
    private String eventData;

    /**
     * 目标服务 (可选，为空表示广播)
     */
    private String targetService;

    public EventMessage() {
        super();
    }

    public EventMessage(String eventType, String eventData) {
        super();
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public EventMessage(String eventType, String eventData, String targetService) {
        super();
        this.eventType = eventType;
        this.eventData = eventData;
        this.targetService = targetService;
    }
}
