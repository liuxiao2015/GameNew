package com.game.core.event;

import com.game.common.util.JsonUtil;
import com.game.mq.MqExchange;
import com.game.mq.consumer.MqListener;
import com.game.mq.message.EventMessage;
import com.game.mq.producer.MqProducer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 RabbitMQ 的分布式事件总线
 * <p>
 * 替代 Redis Pub/Sub 实现，降低 Redis 压力：
 * <ul>
 *     <li>使用 RabbitMQ fanout exchange 实现广播</li>
 *     <li>使用 RabbitMQ topic exchange 实现定向发布</li>
 *     <li>自动与本地 EventBus 集成</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 广播事件到所有服务
 * mqDistributedEventBus.broadcast(new ServerNoticeEvent("维护公告", "服务器将于..."));
 *
 * // 发布到特定服务
 * mqDistributedEventBus.publish("service-game", new ConfigReloadEvent("item.json"));
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class MqDistributedEventBus {

    private final MqProducer mqProducer;
    private final EventBus localEventBus;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 事件类型注册表 (className -> Class)
     */
    private final Map<String, Class<? extends GameEvent>> eventTypeRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("MqDistributedEventBus 初始化完成: applicationName={}", applicationName);
    }

    /**
     * 注册事件类型 (用于反序列化)
     */
    public void registerEventType(Class<? extends GameEvent> eventClass) {
        eventTypeRegistry.put(eventClass.getName(), eventClass);
        log.debug("注册分布式事件类型: {}", eventClass.getSimpleName());
    }

    /**
     * 广播事件到所有服务实例 (使用 fanout exchange)
     */
    public void broadcast(GameEvent event) {
        if (event == null) {
            return;
        }

        EventMessage message = new EventMessage();
        message.setEventType(event.getClass().getName());
        message.setEventData(JsonUtil.toJson(event));
        message.setSourceService(applicationName);

        mqProducer.broadcast(MqExchange.EVENT_BROADCAST, message);

        log.debug("广播分布式事件: type={}", event.getEventType());
    }

    /**
     * 发布全局事件（通过事件类型和数据字符串）
     */
    public void publishGlobal(String eventType, String eventData) {
        EventMessage message = new EventMessage();
        message.setEventType(eventType);
        message.setEventData(eventData);
        message.setSourceService(applicationName);

        mqProducer.broadcast(MqExchange.EVENT_BROADCAST, message);

        log.debug("广播全局事件: type={}", eventType);
    }

    /**
     * 发布事件到特定服务 (使用 topic exchange)
     */
    public void publish(String targetService, GameEvent event) {
        if (event == null || targetService == null) {
            return;
        }

        EventMessage message = new EventMessage();
        message.setEventType(event.getClass().getName());
        message.setEventData(JsonUtil.toJson(event));
        message.setSourceService(applicationName);
        message.setTargetService(targetService);

        // routing key 格式: service.{serviceName}
        String routingKey = "service." + targetService;
        mqProducer.send(MqExchange.EVENT_TOPIC, routingKey, message);

        log.debug("发布分布式事件: target={}, type={}", targetService, event.getEventType());
    }

    /**
     * 处理接收到的广播事件
     */
    @MqListener(exchange = MqExchange.EVENT_BROADCAST)
    public void onBroadcastEvent(EventMessage message) {
        processEventMessage(message);
    }

    /**
     * 处理接收到的定向事件
     */
    @MqListener(exchange = MqExchange.EVENT_TOPIC, routingKey = "service.${spring.application.name}")
    public void onTargetedEvent(EventMessage message) {
        processEventMessage(message);
    }

    /**
     * 处理事件消息
     */
    private void processEventMessage(EventMessage message) {
        try {
            if (message == null) {
                return;
            }

            // 忽略自己发送的消息 (避免重复处理)
            if (applicationName.equals(message.getSourceService())) {
                return;
            }

            // 反序列化事件
            String eventClassName = message.getEventType();
            Class<? extends GameEvent> eventClass = eventTypeRegistry.get(eventClassName);
            
            if (eventClass == null) {
                // 尝试通过反射加载
                try {
                    eventClass = (Class<? extends GameEvent>) Class.forName(eventClassName);
                    eventTypeRegistry.put(eventClassName, eventClass);
                } catch (ClassNotFoundException e) {
                    log.warn("未知的分布式事件类型: {}", eventClassName);
                    return;
                }
            }

            GameEvent event = JsonUtil.fromJson(message.getEventData(), eventClass);
            if (event != null) {
                // 转发到本地事件总线
                localEventBus.publish(event);
                log.debug("接收分布式事件: source={}, type={}",
                        message.getSourceService(), event.getEventType());
            }

        } catch (Exception e) {
            log.error("处理分布式事件异常", e);
        }
    }
}
