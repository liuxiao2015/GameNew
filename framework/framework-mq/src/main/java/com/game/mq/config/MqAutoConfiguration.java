package com.game.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * MQ 自动配置
 * <p>
 * 提供 RabbitMQ 相关 Bean 的自动配置
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Configuration
@ComponentScan("com.game.mq")
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class MqAutoConfiguration {

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;

    @Value("${spring.rabbitmq.publisher-confirms:false}")
    private boolean publisherConfirms;

    @Value("${spring.rabbitmq.publisher-returns:false}")
    private boolean publisherReturns;

    /**
     * 连接工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        
        if (publisherConfirms) {
            factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        }
        factory.setPublisherReturns(publisherReturns);
        
        log.info("RabbitMQ 连接工厂初始化: host={}, port={}, vhost={}", host, port, virtualHost);
        return factory;
    }

    /**
     * RabbitTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(publisherReturns);
        
        if (publisherReturns) {
            template.setReturnsCallback(returned -> {
                log.warn("消息退回: exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText());
            });
        }
        
        return template;
    }

    /**
     * AMQP 管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}
