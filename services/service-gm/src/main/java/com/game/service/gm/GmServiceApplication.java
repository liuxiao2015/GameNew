package com.game.service.gm;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * GM 运营后台服务启动类
 *
 * @author GameServer
 */
@SpringBootApplication
@EnableDubbo
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
