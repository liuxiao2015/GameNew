package com.game.actor.core;

import lombok.Data;

import java.util.concurrent.CompletableFuture;

/**
 * Actor 消息
 *
 * @author GameServer
 */
@Data
public class ActorMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息数据
     */
    private Object data;

    /**
     * 来源 Actor ID (可选)
     */
    private Long sourceActorId;

    /**
     * 来源 Actor 类型 (可选)
     */
    private String sourceActorType;

    /**
     * 消息创建时间
     */
    private long createTime;

    /**
     * 回调 (可选)
     */
    private transient ActorCallback callback;

    /**
     * 异步结果 Future (可选，用于 ask 模式)
     */
    private transient CompletableFuture<Object> future;

    public ActorMessage() {
        this.createTime = System.currentTimeMillis();
    }

    public ActorMessage(String type) {
        this();
        this.type = type;
    }

    public ActorMessage(String type, Object data) {
        this();
        this.type = type;
        this.data = data;
    }

    public ActorMessage(String type, Object data, CompletableFuture<Object> future) {
        this();
        this.type = type;
        this.data = data;
        this.future = future;
    }

    /**
     * 创建消息
     */
    public static ActorMessage of(String type) {
        return new ActorMessage(type);
    }

    /**
     * 创建消息 (带数据)
     */
    public static ActorMessage of(String type, Object data) {
        return new ActorMessage(type, data);
    }

    /**
     * 创建消息 (带数据和 Future)
     */
    public static ActorMessage of(String type, Object data, CompletableFuture<Object> future) {
        return new ActorMessage(type, data, future);
    }

    /**
     * 设置来源
     */
    public ActorMessage from(Long sourceActorId, String sourceActorType) {
        this.sourceActorId = sourceActorId;
        this.sourceActorType = sourceActorType;
        return this;
    }

    /**
     * 设置回调
     */
    public ActorMessage callback(ActorCallback callback) {
        this.callback = callback;
        return this;
    }

    /**
     * 获取数据并转换类型
     */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    /**
     * Actor 回调接口
     */
    @FunctionalInterface
    public interface ActorCallback {
        void onComplete(Object result, Throwable error);
    }

    // ==================== 预定义消息类型 ====================

    /**
     * 保存数据
     */
    public static final String TYPE_SAVE = "SAVE";

    /**
     * 停止 Actor
     */
    public static final String TYPE_STOP = "STOP";

    /**
     * 心跳
     */
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";

    /**
     * 每日重置
     */
    public static final String TYPE_DAILY_RESET = "DAILY_RESET";

    /**
     * 每周重置
     */
    public static final String TYPE_WEEKLY_RESET = "WEEKLY_RESET";
}
