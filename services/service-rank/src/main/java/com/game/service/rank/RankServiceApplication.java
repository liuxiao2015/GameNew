package com.game.service.rank;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 排行服务启动类
 *
 * @author GameServer
 */
@SpringBootApplication
@EnableDubbo
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
