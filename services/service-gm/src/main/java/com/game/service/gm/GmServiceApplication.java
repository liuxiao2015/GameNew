package com.game.service.gm;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * GM 运营后台服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-gm", order = 200, description = "GM后台服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.core",
        "com.game.data",
        "com.game.log",
        "com.game.service.gm",
        "com.game.support.gm"
})
public class GmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmServiceApplication.class, args);
    }
}
