package com.game.core.event;

import com.game.common.util.JsonUtil;
import com.game.data.redis.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式事件总线
 * <p>
 * 基于 Redis Pub/Sub 实现跨服务、跨机器的事件广播：
 * <ul>
 *     <li>支持发布事件到所有服务实例</li>
 *     <li>支持按服务名订阅特定事件</li>
 *     <li>自动与本地 EventBus 集成</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用场景：
 * - 全服公告
 * - 跨服活动通知
 * - 配置热更新通知
 * - 玩家在其他服务器上的状态变更
 * </pre>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 广播事件到所有服务
 * distributedEventBus.broadcast(new ServerNoticeEvent("维护公告", "服务器将于..."));
 *
 * // 发布到特定服务
 * distributedEventBus.publish("service-game", new ConfigReloadEvent("item.json"));
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedEventBus implements MessageListener {

    private final RedisService redisService;
    private final EventBus localEventBus;
    private final RedisMessageListenerContainer listenerContainer;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 全局广播 Channel
     */
    private static final String BROADCAST_CHANNEL = "event:broadcast";

    /**
     * 服务专属 Channel 前缀
     */
    private static final String SERVICE_CHANNEL_PREFIX = "event:service:";

    /**
     * 事件类型注册表 (className -> Class)
     */
    private final Map<String, Class<? extends GameEvent>> eventTypeRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 订阅全局广播 Channel
        listenerContainer.addMessageListener(this, new PatternTopic(BROADCAST_CHANNEL));

        // 订阅本服务专属 Channel
        String serviceChannel = SERVICE_CHANNEL_PREFIX + applicationName;
        listenerContainer.addMessageListener(this, new PatternTopic(serviceChannel));

        log.info("DistributedEventBus 初始化完成: applicationName={}", applicationName);
    }

    /**
     * 注册事件类型 (用于反序列化)
     */
    public void registerEventType(Class<? extends GameEvent> eventClass) {
        eventTypeRegistry.put(eventClass.getName(), eventClass);
        log.debug("注册分布式事件类型: {}", eventClass.getSimpleName());
    }

    /**
     * 广播事件到所有服务实例
     */
    public void broadcast(GameEvent event) {
        if (event == null) {
            return;
        }

        DistributedEventMessage message = new DistributedEventMessage();
        message.setEventClassName(event.getClass().getName());
        message.setEventData(JsonUtil.toJson(event));
        message.setSourceService(applicationName);
        message.setTimestamp(System.currentTimeMillis());

        String json = JsonUtil.toJson(message);
        redisService.publish(BROADCAST_CHANNEL, json);

        log.debug("广播分布式事件: type={}", event.getEventType());
    }

    /**
     * 发布全局事件（通过事件类型和数据字符串）
     * 
     * @param eventType 事件类型
     * @param eventData 事件数据 (JSON)
     */
    public void publishGlobal(String eventType, String eventData) {
        DistributedEventMessage message = new DistributedEventMessage();
        message.setEventClassName(eventType);
        message.setEventData(eventData);
        message.setSourceService(applicationName);
        message.setTimestamp(System.currentTimeMillis());

        String json = JsonUtil.toJson(message);
        redisService.publish(BROADCAST_CHANNEL, json);

        log.debug("广播全局事件: type={}", eventType);
    }

    /**
     * 发布事件到特定服务
     */
    public void publish(String targetService, GameEvent event) {
        if (event == null || targetService == null) {
            return;
        }

        DistributedEventMessage message = new DistributedEventMessage();
        message.setEventClassName(event.getClass().getName());
        message.setEventData(JsonUtil.toJson(event));
        message.setSourceService(applicationName);
        message.setTargetService(targetService);
        message.setTimestamp(System.currentTimeMillis());

        String channel = SERVICE_CHANNEL_PREFIX + targetService;
        String json = JsonUtil.toJson(message);
        redisService.publish(channel, json);

        log.debug("发布分布式事件: target={}, type={}", targetService, event.getEventType());
    }

    /**
     * 处理接收到的消息
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            DistributedEventMessage eventMessage = JsonUtil.fromJson(json, DistributedEventMessage.class);

            if (eventMessage == null) {
                return;
            }

            // 忽略自己发送的消息 (避免重复处理)
            if (applicationName.equals(eventMessage.getSourceService())) {
                return;
            }

            // 反序列化事件
            Class<? extends GameEvent> eventClass = eventTypeRegistry.get(eventMessage.getEventClassName());
            if (eventClass == null) {
                // 尝试通过反射加载
                try {
                    eventClass = (Class<? extends GameEvent>) Class.forName(eventMessage.getEventClassName());
                    eventTypeRegistry.put(eventMessage.getEventClassName(), eventClass);
                } catch (ClassNotFoundException e) {
                    log.warn("未知的分布式事件类型: {}", eventMessage.getEventClassName());
                    return;
                }
            }

            GameEvent event = JsonUtil.fromJson(eventMessage.getEventData(), eventClass);
            if (event != null) {
                // 转发到本地事件总线
                localEventBus.publish(event);
                log.debug("接收分布式事件: source={}, type={}",
                        eventMessage.getSourceService(), event.getEventType());
            }

        } catch (Exception e) {
            log.error("处理分布式事件异常", e);
        }
    }

    /**
     * 分布式事件消息
     */
    @lombok.Data
    public static class DistributedEventMessage {
        private String eventClassName;
        private String eventData;
        private String sourceService;
        private String targetService;
        private long timestamp;
    }
}
