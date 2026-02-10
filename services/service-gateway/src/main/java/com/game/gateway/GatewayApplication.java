package com.game.gateway;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 网关服务启动类
 *
 * @author GameServer
 */
@GameService(name = "service-gateway", order = 10, description = "网关服务")
@SpringBootApplication
@EnableDubbo
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.core",
        "com.game.data",
        "com.game.gateway"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
