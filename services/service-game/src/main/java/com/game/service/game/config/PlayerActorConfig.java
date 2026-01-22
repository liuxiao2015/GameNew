package com.game.service.game.config;

import com.game.actor.core.ActorSystem;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.game.service.game.actor.PlayerActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 玩家 Actor 配置
 *
 * @author GameServer
 */
@Slf4j
@Configuration
public class PlayerActorConfig {

    @Bean
    @ConfigurationProperties(prefix = "game.actor.player")
    public ActorSystem.ActorSystemConfig playerActorConfig() {
        return ActorSystem.ActorSystemConfig.create();
    }

    @Bean(name = "playerActorSystem")
    @ConditionalOnBean({RedisService.class, MongoService.class})
    public ActorSystem<PlayerActor> playerActorSystem(
            ActorSystem.ActorSystemConfig playerActorConfig,
            RedisService redisService,
            MongoService mongoService) {

        log.info("初始化玩家 ActorSystem: maxSize={}, idleTimeout={}min",
                playerActorConfig.getMaxSize(), playerActorConfig.getIdleTimeoutMinutes());

        return new ActorSystem<>("player", playerActorConfig,
                roleId -> new PlayerActor(roleId, redisService, mongoService));
    }
}
