package com.game.mq.consumer;

import com.game.mq.MqExchange;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * MQ 消息监听器注解
 * <p>
 * 标注在方法上，用于接收 MQ 消息
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @MqListener(exchange = MqExchange.CHAT_WORLD)
 * public void onWorldChat(ChatMqMessage message) {
 *     // 处理世界聊天消息
 * }
 *
 * @MqListener(exchange = MqExchange.EVENT_TOPIC, routingKey = "service.game.*")
 * public void onGameEvent(EventMessage message) {
 *     // 处理游戏服务事件
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MqListener {

    /**
     * Exchange 名称
     */
    MqExchange exchange();

    /**
     * 路由键 (支持通配符)
     * 对于 FANOUT 类型可留空
     */
    String routingKey() default "";

    /**
     * 队列名称 (默认自动生成)
     * 格式: {exchange}.{applicationName}
     */
    String queue() default "";

    /**
     * 并发消费者数量
     */
    int concurrency() default 1;

    /**
     * 是否自动确认
     */
    boolean autoAck() default true;
}
