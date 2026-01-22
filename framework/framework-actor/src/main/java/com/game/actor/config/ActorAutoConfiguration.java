package com.game.actor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Actor 模块自动配置
 * <p>
 * 框架层只提供基础 Actor 抽象，具体的 ActorSystem 实例应该由各业务服务创建
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Configuration
@ComponentScan("com.game.actor")
public class ActorAutoConfiguration {

    // ActorSystem 的创建由各业务服务负责
    // 例如：
    // - service-game 创建 PlayerActor 的 ActorSystem
    // - service-guild 创建 GuildActor 的 ActorSystem
}
