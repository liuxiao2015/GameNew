package com.game.core.monitor;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 * <p>
 * 提供 HTTP 健康检查端点，用于：
 * <ul>
 *     <li>K8s/Docker 健康检查</li>
 *     <li>负载均衡器健康探测</li>
 *     <li>监控系统采集</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final ServerMonitor serverMonitor;

    /**
     * 存活探针 (Liveness)
     * <p>
     * 返回 200 表示服务存活
     * </p>
     */
    @GetMapping("/live")
    public Map<String, Object> liveness() {
        return Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 就绪探针 (Readiness)
     * <p>
     * 检查服务是否可以接收请求
     * </p>
     */
    @GetMapping("/ready")
    public Map<String, Object> readiness() {
        boolean healthy = serverMonitor.isHealthy();
        return Map.of(
                "status", healthy ? "UP" : "DOWN",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 完整健康状态
     */
    @GetMapping
    public ServerMonitor.HealthStatus health() {
        return serverMonitor.getHealthStatus();
    }

    /**
     * 请求统计
     */
    @GetMapping("/stats")
    public ServerMonitor.RequestStats stats() {
        return serverMonitor.getRequestStats();
    }
}
