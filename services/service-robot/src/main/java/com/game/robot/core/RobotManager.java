package com.game.robot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 机器人管理器
 * <p>
 * 管理多个机器人实例，用于压力测试
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class RobotManager {

    @Value("${robot.server.host:127.0.0.1}")
    private String serverHost;

    @Value("${robot.server.port:8888}")
    private int serverPort;

    private final Map<Integer, Robot> robots = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 创建并启动指定数量的机器人
     */
    public CompletableFuture<Void> startRobots(int count, long delayMs) {
        log.info("开始启动 {} 个机器人, 间隔 {}ms", count, delayMs);

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (int i = 0; i < count; i++) {
            final int robotId = i + 1;

            future = future.thenCompose(v -> {
                Robot robot = new Robot(robotId, serverHost, serverPort);
                robots.put(robotId, robot);

                return robot.start()
                        .thenCompose(v2 -> delay(delayMs))
                        .exceptionally(ex -> {
                            log.error("机器人 {} 启动失败", robotId, ex);
                            return null;
                        });
            });
        }

        return future.thenRun(() -> {
            log.info("所有机器人启动完成, 共 {} 个", robots.size());
            // 启动心跳任务
            startHeartbeat();
        });
    }

    /**
     * 停止所有机器人
     */
    @PreDestroy
    public void stopAll() {
        log.info("停止所有机器人...");
        robots.values().forEach(Robot::stop);
        robots.clear();
        scheduler.shutdown();
    }

    /**
     * 获取机器人
     */
    public Robot getRobot(int robotId) {
        return robots.get(robotId);
    }

    /**
     * 获取在线机器人数量
     */
    public int getOnlineCount() {
        return (int) robots.values().stream()
                .filter(r -> r.getClient().isConnected())
                .count();
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            robots.values().forEach(Robot::sendHeartbeat);
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 延迟
     */
    private CompletableFuture<Void> delay(long ms) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.schedule(() -> future.complete(null), ms, TimeUnit.MILLISECONDS);
        return future;
    }

    /**
     * 让所有机器人执行动作
     */
    public void broadcast(int protocolId, int methodId, byte[] data) {
        robots.values().forEach(robot -> {
            if (robot.getClient().isConnected() && robot.isLoggedIn()) {
                robot.action(protocolId, methodId, data);
            }
        });
    }
}
