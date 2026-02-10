package com.game.service.login;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 登录服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-login", order = 20, description = "登录服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.log",
        "com.game.service.login"
})
public class LoginServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginServiceApplication.class, args);
    }
}
