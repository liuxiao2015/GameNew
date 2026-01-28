package com.game.service.battle;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 战斗服务启动类
 *
 * @author GameServer
 */
@SpringBootApplication
@EnableDubbo
@EnableScheduling
@ComponentScan(basePackages = {"com.game.core", "com.game.common", "com.game.mq", "com.game.service.battle"})
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
public class BattleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleServiceApplication.class, args);
    }
}
