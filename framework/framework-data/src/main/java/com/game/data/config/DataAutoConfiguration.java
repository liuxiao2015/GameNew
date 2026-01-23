package com.game.data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * 数据访问层自动配置
 *
 * @author GameServer
 */
@Configuration
@ComponentScan(basePackages = "com.game.data")
@EnableMongoRepositories(basePackages = "com.game.data.mongo.repository")
@EnableRedisRepositories(basePackages = "com.game.data.redis.repository")
public class DataAutoConfiguration {

    /**
     * Redis 消息监听容器
     * <p>
     * 用于分布式事件总线
     * </p>
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
