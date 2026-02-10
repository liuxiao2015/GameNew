package com.game.service.scheduler;

import com.game.common.launcher.GameService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 调度服务启动类
 * <p>
 * 负责定时任务、延时任务、周期任务的调度和执行
 * </p>
 *
 * @author GameServer
 */
@GameService(name = "service-scheduler", order = 70, description = "定时任务服务")
@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.game.entity.repository")
@ComponentScan(basePackages = {
        "com.game.common",
        "com.game.data",
        "com.game.service.scheduler"
})
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
