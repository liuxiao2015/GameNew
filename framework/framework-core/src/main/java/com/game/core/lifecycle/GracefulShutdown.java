package com.game.core.lifecycle;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 优雅停机管理
 * <p>
 * 提供统一的服务停机处理：
 * <ul>
 *     <li>按优先级执行停机任务</li>
 *     <li>等待所有任务完成</li>
 *     <li>超时保护机制</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Component
 * public class PlayerService implements ShutdownAware {
 *     @Override
 *     public int getShutdownOrder() {
 *         return 100; // 数字越大越先执行
 *     }
 *
 *     @Override
 *     public void onShutdown() {
 *         // 保存所有在线玩家数据
 *         saveAllPlayers();
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdown {

    private final ApplicationContext applicationContext;

    /**
     * 最大等待时间（秒）
     */
    private static final int MAX_WAIT_SECONDS = 30;

    /**
     * 是否正在停机
     */
    private volatile boolean shuttingDown = false;

    @PreDestroy
    public void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;

        log.info("============ 开始优雅停机 ============");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 收集所有需要处理停机的组件
            List<ShutdownAware> handlers = new ArrayList<>(
                    applicationContext.getBeansOfType(ShutdownAware.class).values()
            );

            // 2. 按优先级排序（数字大的先执行）
            handlers.sort(Comparator.comparingInt(ShutdownAware::getShutdownOrder).reversed());

            log.info("发现 {} 个停机处理器", handlers.size());

            // 3. 依次执行停机任务
            CountDownLatch latch = new CountDownLatch(handlers.size());

            for (ShutdownAware handler : handlers) {
                try {
                    String name = handler.getClass().getSimpleName();
                    log.info("执行停机: {} (order={})", name, handler.getShutdownOrder());

                    long taskStart = System.currentTimeMillis();
                    handler.onShutdown();
                    long taskCost = System.currentTimeMillis() - taskStart;

                    log.info("停机完成: {} (cost={}ms)", name, taskCost);

                } catch (Exception e) {
                    log.error("停机处理异常: {}", handler.getClass().getSimpleName(), e);
                } finally {
                    latch.countDown();
                }
            }

            // 4. 等待所有任务完成
            boolean completed = latch.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("停机任务未能在 {}s 内完成", MAX_WAIT_SECONDS);
            }

        } catch (Exception e) {
            log.error("优雅停机异常", e);
        }

        long totalCost = System.currentTimeMillis() - startTime;
        log.info("============ 优雅停机完成 (cost={}ms) ============", totalCost);
    }

    /**
     * 判断是否正在停机
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }
}
