package com.game.service.guild;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 公会服务启动类
 *
 * @author GameServer
 */
@SpringBootApplication
@EnableDubbo
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.actor",
        "com.game.log",
        "com.game.service.guild"
})
public class GuildServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuildServiceApplication.class, args);
    }
}
