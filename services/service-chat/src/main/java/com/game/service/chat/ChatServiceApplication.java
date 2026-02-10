package com.game.service.chat;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 聊天服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-chat", order = 50, description = "聊天服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.log",
        "com.game.service.chat"
})
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
