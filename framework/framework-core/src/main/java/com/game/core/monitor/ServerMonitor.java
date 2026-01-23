package com.game.core.monitor;

import com.game.core.net.session.SessionManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器监控
 * <p>
 * 生产级监控能力：
 * <ul>
 *     <li>服务器健康状态</li>
 *     <li>JVM 内存监控</li>
 *     <li>CPU 负载监控</li>
 *     <li>请求统计</li>
 *     <li>自动告警</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerMonitor {

    private final SessionManager sessionManager;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 启动时间
     */
    private final Instant startTime = Instant.now();

    /**
     * 请求计数器
     */
    private final AtomicLong requestCount = new AtomicLong(0);

    /**
     * 成功请求计数
     */
    private final AtomicLong successCount = new AtomicLong(0);

    /**
     * 失败请求计数
     */
    private final AtomicLong failCount = new AtomicLong(0);

    /**
     * 内存警告阈值 (80%)
     */
    private static final double MEMORY_WARN_THRESHOLD = 0.8;

    /**
     * CPU 警告阈值 (80%)
     */
    private static final double CPU_WARN_THRESHOLD = 0.8;

    // ==================== 请求统计 ====================

    /**
     * 记录请求
     */
    public void recordRequest(boolean success) {
        requestCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failCount.incrementAndGet();
        }
    }

    /**
     * 获取请求统计
     */
    public RequestStats getRequestStats() {
        return new RequestStats(
                requestCount.get(),
                successCount.get(),
                failCount.get()
        );
    }

    // ==================== 健康检查 ====================

    /**
     * 获取健康状态
     */
    public HealthStatus getHealthStatus() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // 内存使用
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = heapMax > 0 ? (double) heapUsed / heapMax : 0;

        // CPU 负载
        double cpuLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        double cpuUsage = cpuLoad >= 0 ? cpuLoad / availableProcessors : 0;

        // 线程数
        int threadCount = threadBean.getThreadCount();

        // 在线人数
        int onlineCount = sessionManager != null ? sessionManager.getOnlineCount() : 0;

        // 运行时长
        Duration uptime = Duration.between(startTime, Instant.now());

        // 判断状态
        String status = "UP";
        if (memoryUsage > MEMORY_WARN_THRESHOLD || cpuUsage > CPU_WARN_THRESHOLD) {
            status = "WARN";
        }

        return new HealthStatus(
                status,
                applicationName,
                serverPort,
                uptime.toSeconds(),
                heapUsed / (1024 * 1024),
                heapMax / (1024 * 1024),
                memoryUsage,
                cpuUsage,
                threadCount,
                onlineCount,
                requestCount.get()
        );
    }

    /**
     * 检查是否健康
     */
    public boolean isHealthy() {
        HealthStatus status = getHealthStatus();
        return "UP".equals(status.status()) || "WARN".equals(status.status());
    }

    // ==================== 定时检查 ====================

    /**
     * 定时监控 (每分钟)
     */
    @Scheduled(fixedRate = 60000)
    public void monitor() {
        HealthStatus status = getHealthStatus();

        // 记录状态
        log.info("服务器状态: status={}, memory={}/{}MB ({:.1f}%), cpu={:.1f}%, threads={}, online={}, requests={}",
                status.status(),
                status.heapUsedMb(),
                status.heapMaxMb(),
                status.memoryUsage() * 100,
                status.cpuUsage() * 100,
                status.threadCount(),
                status.onlineCount(),
                status.requestCount());

        // 告警检查
        if (status.memoryUsage() > MEMORY_WARN_THRESHOLD) {
            log.warn("内存使用率过高: {:.1f}%", status.memoryUsage() * 100);
        }
        if (status.cpuUsage() > CPU_WARN_THRESHOLD) {
            log.warn("CPU 使用率过高: {:.1f}%", status.cpuUsage() * 100);
        }
    }

    // ==================== 数据类 ====================

    public record HealthStatus(
            String status,
            String application,
            int port,
            long uptimeSeconds,
            long heapUsedMb,
            long heapMaxMb,
            double memoryUsage,
            double cpuUsage,
            int threadCount,
            int onlineCount,
            long requestCount
    ) {
        public String getUptimeFormatted() {
            long days = uptimeSeconds / 86400;
            long hours = (uptimeSeconds % 86400) / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            return String.format("%dd %dh %dm", days, hours, minutes);
        }
    }

    public record RequestStats(
            long total,
            long success,
            long fail
    ) {
        public double getSuccessRate() {
            return total > 0 ? (double) success / total : 1.0;
        }
    }
}
