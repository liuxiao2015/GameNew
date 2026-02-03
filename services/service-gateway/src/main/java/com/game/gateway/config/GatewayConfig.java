package com.game.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置
 *
 * @author GameServer
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfig {

    /**
     * TCP 配置
     */
    private TcpConfig tcp = new TcpConfig();

    /**
     * WebSocket 配置
     */
    private WebSocketConfig websocket = new WebSocketConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class TcpConfig {
        private boolean enabled = true;
        private int port = 9000;
        private int bossThreads = 1;
        private int workerThreads = 0;
        private int backlog = 1024;
        private HeartbeatConfig heartbeat = new HeartbeatConfig();
    }

    @Data
    public static class HeartbeatConfig {
        private int intervalSeconds = 30;
        private int timeoutSeconds = 90;
    }

    @Data
    public static class WebSocketConfig {
        private boolean enabled = true;
        private int port = 9001;
        private String path = "/ws";
    }

    @Data
    public static class RateLimitConfig {
        private boolean enabled = true;
        private int globalQps = 10000;
        private int ipQps = 100;
        private int userQps = 50;
    }
}
