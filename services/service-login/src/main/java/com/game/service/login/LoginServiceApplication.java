package com.game.service.login;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 登录服务启动类
 *
 * @author GameServer
 */
@SpringBootApplication
@EnableDubbo
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
