package com.game.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.mq.MqExchange;
import com.game.mq.message.MqMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MQ 监听器注册中心
 * <p>
 * 扫描 @MqListener 注解并自动注册消费者
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqListenerRegistry {

    private final ApplicationContext applicationContext;
    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 声明所有 Exchange
        declareExchanges();
        
        // 扫描并注册监听器
        scanListeners();
        
        log.info("MQ 监听器注册完成，共注册 {} 个", containers.size());
    }

    /**
     * 声明所有 Exchange
     */
    private void declareExchanges() {
        for (MqExchange exchange : MqExchange.values()) {
            Exchange ex = switch (exchange.getType()) {
                case FANOUT -> new FanoutExchange(exchange.getName(), true, false);
                case DIRECT -> new DirectExchange(exchange.getName(), true, false);
                case TOPIC -> new TopicExchange(exchange.getName(), true, false);
                case HEADERS -> new HeadersExchange(exchange.getName(), true, false);
            };
            amqpAdmin.declareExchange(ex);
            log.debug("声明 Exchange: name={}, type={}", exchange.getName(), exchange.getType());
        }
    }

    /**
     * 扫描并注册监听器
     */
    private void scanListeners() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

        for (Object bean : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {
                MqListener annotation = method.getAnnotation(MqListener.class);
                if (annotation != null) {
                    registerListener(bean, method, annotation);
                }
            }
        }
    }

    /**
     * 注册单个监听器
     */
    private void registerListener(Object bean, Method method, MqListener annotation) {
        MqExchange exchange = annotation.exchange();
        String routingKey = annotation.routingKey();
        String configuredQueueName = annotation.queue();
        
        // 生成队列名
        final String queueName;
        if (configuredQueueName.isEmpty()) {
            String generatedName = exchange.getName() + "." + applicationName;
            if (!routingKey.isEmpty()) {
                generatedName += "." + routingKey.replace("*", "any").replace("#", "all");
            }
            queueName = generatedName;
        } else {
            queueName = configuredQueueName;
        }
        
        // 声明队列
        Queue queue = new Queue(queueName, true, false, false);
        amqpAdmin.declareQueue(queue);
        
        // 绑定队列到 Exchange
        Binding binding = switch (exchange.getType()) {
            case FANOUT -> BindingBuilder.bind(queue).to(new FanoutExchange(exchange.getName()));
            case DIRECT -> BindingBuilder.bind(queue).to(new DirectExchange(exchange.getName()))
                    .with(routingKey.isEmpty() ? queueName : routingKey);
            case TOPIC -> BindingBuilder.bind(queue).to(new TopicExchange(exchange.getName()))
                    .with(routingKey.isEmpty() ? "#" : routingKey);
            case HEADERS -> BindingBuilder.bind(queue).to(new HeadersExchange(exchange.getName()))
                    .whereAll(Map.of()).match();
        };
        amqpAdmin.declareBinding(binding);
        
        // 获取方法参数类型
        Parameter[] params = method.getParameters();
        if (params.length != 1 || !MqMessage.class.isAssignableFrom(params[0].getType())) {
            log.warn("MqListener 方法参数错误，必须有且只有一个 MqMessage 子类参数: {}.{}",
                    bean.getClass().getSimpleName(), method.getName());
            return;
        }
        Class<?> messageType = params[0].getType();
        
        // 创建监听容器
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(annotation.concurrency());
        container.setAcknowledgeMode(annotation.autoAck() ? AcknowledgeMode.AUTO : AcknowledgeMode.MANUAL);
        
        container.setMessageListener((MessageListener) message -> {
            try {
                String json = new String(message.getBody());
                Object msg = objectMapper.readValue(json, messageType);
                method.invoke(bean, msg);
            } catch (Exception e) {
                log.error("处理MQ消息失败: queue={}, method={}.{}", 
                        queueName, bean.getClass().getSimpleName(), method.getName(), e);
            }
        });
        
        container.start();
        containers.add(container);
        
        log.info("注册 MQ 监听器: exchange={}, queue={}, method={}.{}", 
                exchange.getName(), queueName, bean.getClass().getSimpleName(), method.getName());
    }

    /**
     * 停止所有监听器
     */
    public void shutdown() {
        containers.forEach(SimpleMessageListenerContainer::stop);
        log.info("MQ 监听器已停止");
    }
}
