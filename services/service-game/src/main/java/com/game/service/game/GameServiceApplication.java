package com.game.service.game;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 游戏服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-game", order = 30, description = "游戏核心服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.actor",
        "com.game.log",
        "com.game.service.game"
})
public class GameServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
