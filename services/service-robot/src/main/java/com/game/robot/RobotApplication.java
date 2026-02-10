package com.game.robot;

import com.game.common.launcher.GameService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 机器人服务启动类
 * <p>
 * 用于压测、自动化测试、模拟客户端行为
 * </p>
 *
 * @author GameServer
 */
@GameService(name = "service-robot", order = 300, description = "机器人服务", enabled = false)
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.core",
        "com.game.robot"
})
public class RobotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotApplication.class, args);
    }
}
