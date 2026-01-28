package com.game.mq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.mq.MqExchange;
import com.game.mq.message.MqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 * <p>
 * 封装 RabbitMQ 消息发送
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 发送消息到指定 Exchange (不指定 routing key)
     *
     * @param exchange 交换机
     * @param message  消息
     */
    public void send(MqExchange exchange, MqMessage message) {
        send(exchange, "", message);
    }

    /**
     * 发送消息到指定 Exchange
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public void send(MqExchange exchange, String routingKey, MqMessage message) {
        try {
            message.setSourceService(applicationName);
            String json = objectMapper.writeValueAsString(message);
            
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setMessageId(message.getMessageId());
            props.setTimestamp(new java.util.Date(message.getTimestamp()));
            props.setHeader("messageType", message.getMessageType());
            props.setHeader("sourceService", applicationName);
            
            Message amqpMessage = new Message(json.getBytes(), props);
            rabbitTemplate.send(exchange.getName(), routingKey, amqpMessage);
            
            log.debug("发送MQ消息: exchange={}, routingKey={}, messageId={}", 
                    exchange.getName(), routingKey, message.getMessageId());
        } catch (Exception e) {
            log.error("发送MQ消息失败: exchange={}, routingKey={}", 
                    exchange.getName(), routingKey, e);
        }
    }

    /**
     * 发送消息并等待确认
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @return 是否发送成功
     */
    public boolean sendAndConfirm(MqExchange exchange, String routingKey, MqMessage message) {
        try {
            message.setSourceService(applicationName);
            String json = objectMapper.writeValueAsString(message);
            
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setMessageId(message.getMessageId());
            props.setHeader("messageType", message.getMessageType());
            
            Message amqpMessage = new Message(json.getBytes(), props);
            
            rabbitTemplate.invoke(operations -> {
                operations.send(exchange.getName(), routingKey, amqpMessage);
                return operations.waitForConfirms(5000);
            });
            
            log.debug("发送MQ消息并确认: exchange={}, routingKey={}", 
                    exchange.getName(), routingKey);
            return true;
        } catch (Exception e) {
            log.error("发送MQ消息失败: exchange={}, routingKey={}", 
                    exchange.getName(), routingKey, e);
            return false;
        }
    }

    /**
     * 发送延迟消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @param delayMs    延迟时间 (毫秒)
     */
    public void sendDelayed(MqExchange exchange, String routingKey, MqMessage message, long delayMs) {
        try {
            message.setSourceService(applicationName);
            String json = objectMapper.writeValueAsString(message);
            
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setMessageId(message.getMessageId());
            props.setHeader("messageType", message.getMessageType());
            props.setDelay((int) delayMs);  // RabbitMQ delayed message plugin
            
            Message amqpMessage = new Message(json.getBytes(), props);
            rabbitTemplate.send(exchange.getName(), routingKey, amqpMessage);
            
            log.debug("发送延迟MQ消息: exchange={}, routingKey={}, delay={}ms", 
                    exchange.getName(), routingKey, delayMs);
        } catch (Exception e) {
            log.error("发送延迟MQ消息失败: exchange={}, routingKey={}", 
                    exchange.getName(), routingKey, e);
        }
    }

    /**
     * 发送广播消息
     *
     * @param exchange 交换机 (应为 FANOUT 类型)
     * @param message  消息
     */
    public void broadcast(MqExchange exchange, MqMessage message) {
        send(exchange, "", message);
    }
}
