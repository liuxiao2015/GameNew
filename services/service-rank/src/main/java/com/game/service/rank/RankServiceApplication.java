package com.game.service.rank;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 排行服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-rank", order = 60, description = "排行榜服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = {"com.game.entity.repository", "com.game.service.rank.repository"})
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.service.rank"
})
public class RankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankServiceApplication.class, args);
    }
}
