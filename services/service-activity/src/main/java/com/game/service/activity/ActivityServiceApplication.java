package com.game.service.activity;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 活动服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-activity", order = 80, description = "活动服务")
@SpringBootApplication
@EnableDubbo
@EnableAsync
@EnableScheduling
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.core",
        "com.game.data",
        "com.game.service.activity"
})
public class ActivityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }
}
