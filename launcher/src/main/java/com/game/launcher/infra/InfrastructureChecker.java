package com.game.launcher.infra;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基础设施就绪检查器
 * <p>
 * 在启动服务前检查 Nacos、Redis、MongoDB 等是否可用
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class InfrastructureChecker {

    /**
     * 基础设施配置
     */
    public record InfraConfig(String name, String host, int port, CheckType type, String healthUrl) {
        public InfraConfig(String name, String host, int port) {
            this(name, host, port, CheckType.TCP, null);
        }

        public InfraConfig(String name, String host, int port, String healthUrl) {
            this(name, host, port, CheckType.HTTP, healthUrl);
        }
    }

    public enum CheckType {
        TCP, HTTP
    }

    /**
     * 默认基础设施列表
     */
    private static final List<InfraConfig> DEFAULT_INFRA = List.of(
            new InfraConfig("Nacos", "localhost", 8848, "http://localhost:8848/nacos/v1/console/health/readiness"),
            new InfraConfig("Nacos-GRPC", "localhost", 9848),
            new InfraConfig("Redis", "localhost", 6379),
            new InfraConfig("MongoDB", "localhost", 27017),
            new InfraConfig("RabbitMQ", "localhost", 5672)
    );

    private final List<InfraConfig> infraList;
    private final int maxRetries;
    private final int retryIntervalSeconds;

    public InfrastructureChecker() {
        this(DEFAULT_INFRA, 30, 2);
    }

    public InfrastructureChecker(List<InfraConfig> infraList, int maxRetries, int retryIntervalSeconds) {
        this.infraList = infraList;
        this.maxRetries = maxRetries;
        this.retryIntervalSeconds = retryIntervalSeconds;
    }

    /**
     * 检查所有基础设施是否就绪
     *
     * @return 所有检查结果
     */
    public List<CheckResult> checkAll() {
        List<CheckResult> results = new ArrayList<>();
        for (InfraConfig infra : infraList) {
            results.add(check(infra));
        }
        return results;
    }

    /**
     * 检查单个基础设施
     */
    public CheckResult check(InfraConfig infra) {
        boolean available = switch (infra.type()) {
            case TCP -> checkTcp(infra.host(), infra.port());
            case HTTP -> checkHttp(infra.healthUrl());
        };
        return new CheckResult(infra.name(), available, infra.host(), infra.port());
    }

    /**
     * 等待所有基础设施就绪
     *
     * @return 是否全部就绪
     */
    public boolean waitForReady() {
        return waitForReady(infraList);
    }

    /**
     * 等待指定的基础设施就绪
     */
    public boolean waitForReady(List<InfraConfig> infras) {
        log.info("========================================");
        log.info("        检查基础设施就绪状态");
        log.info("========================================");

        List<InfraConfig> pending = new ArrayList<>(infras);

        for (int retry = 0; retry < maxRetries && !pending.isEmpty(); retry++) {
            if (retry > 0) {
                log.info("等待 {}s 后重试... (第 {}/{} 次)", retryIntervalSeconds, retry, maxRetries);
                try {
                    TimeUnit.SECONDS.sleep(retryIntervalSeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            List<InfraConfig> stillPending = new ArrayList<>();
            for (InfraConfig infra : pending) {
                CheckResult result = check(infra);
                if (result.available()) {
                    log.info("  ✓ {} ({}:{}) 已就绪", result.name(), result.host(), result.port());
                } else {
                    stillPending.add(infra);
                    if (retry == 0) {
                        log.warn("  ✗ {} ({}:{}) 未就绪", result.name(), result.host(), result.port());
                    }
                }
            }
            pending = stillPending;
        }

        if (pending.isEmpty()) {
            log.info("========================================");
            log.info("        所有基础设施已就绪");
            log.info("========================================");
            return true;
        } else {
            log.error("========================================");
            log.error("        基础设施检查失败！");
            log.error("========================================");
            for (InfraConfig infra : pending) {
                log.error("  ✗ {} ({}:{}) 未能启动", infra.name(), infra.host(), infra.port());
            }
            return false;
        }
    }

    /**
     * 等待必要的基础设施就绪 (Nacos 和 MongoDB)
     */
    public boolean waitForEssential() {
        List<InfraConfig> essential = infraList.stream()
                .filter(i -> i.name().equals("Nacos") || i.name().equals("Nacos-GRPC") || i.name().equals("MongoDB"))
                .toList();
        return waitForReady(essential);
    }

    /**
     * TCP 端口检查
     */
    private boolean checkTcp(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 3000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * HTTP 健康检查
     */
    private boolean checkHttp(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 快速检查 (不等待)
     */
    public boolean quickCheck() {
        for (InfraConfig infra : infraList) {
            if (!check(infra).available()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 打印状态
     */
    public void printStatus() {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────┐");
        System.out.println("│                  基础设施状态                       │");
        System.out.println("├──────────────────┬────────────────────┬─────────────┤");
        System.out.println("│ 服务             │ 地址               │ 状态        │");
        System.out.println("├──────────────────┼────────────────────┼─────────────┤");

        for (InfraConfig infra : infraList) {
            CheckResult result = check(infra);
            String name = padRight(infra.name(), 16);
            String addr = padRight(infra.host() + ":" + infra.port(), 18);
            String status = result.available() ? "✓ 运行中" : "✗ 未运行";
            String statusPadded = padRight(status, 11);
            System.out.printf("│ %s │ %s │ %s │%n", name, addr, statusPadded);
        }

        System.out.println("└──────────────────┴────────────────────┴─────────────┘");
        System.out.println();
    }

    private String padRight(String str, int length) {
        if (str == null) str = "";
        // 中文字符占2个宽度，需要特殊处理
        int actualLength = 0;
        for (char c : str.toCharArray()) {
            actualLength += (c > 127 ? 2 : 1);
        }
        if (actualLength >= length) return str;
        return str + " ".repeat(length - actualLength);
    }

    /**
     * 检查结果
     */
    public record CheckResult(String name, boolean available, String host, int port) {
    }
}
