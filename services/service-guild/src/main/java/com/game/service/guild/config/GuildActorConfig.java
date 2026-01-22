package com.game.service.guild.config;

import com.game.actor.core.ActorSystem;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.game.service.guild.actor.GuildActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公会 Actor 配置
 *
 * @author GameServer
 */
@Slf4j
@Configuration
public class GuildActorConfig {

    @Bean
    @ConfigurationProperties(prefix = "game.actor.guild")
    public ActorSystem.ActorSystemConfig guildActorConfig() {
        return ActorSystem.ActorSystemConfig.create();
    }

    @Bean(name = "guildActorSystem")
    @ConditionalOnBean({RedisService.class, MongoService.class})
    public ActorSystem<GuildActor> guildActorSystem(
            ActorSystem.ActorSystemConfig guildActorConfig,
            RedisService redisService,
            MongoService mongoService) {

        log.info("初始化公会 ActorSystem: maxSize={}, idleTimeout={}min",
                guildActorConfig.getMaxSize(), guildActorConfig.getIdleTimeoutMinutes());

        return new ActorSystem<>("guild", guildActorConfig,
                guildId -> new GuildActor(guildId, redisService, mongoService));
    }
}
