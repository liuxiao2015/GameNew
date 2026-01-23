package com.game.launcher.core;

import com.game.launcher.config.LauncherConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 服务进程
 *
 * @author GameServer
 */
@Slf4j
@Getter
public class ServiceProcess {

    private final LauncherConfig.ServiceConfig config;
    private final LauncherConfig.GlobalConfig globalConfig;
    private final int instanceIndex;

    private Process process;
    private long pid;
    private volatile ServiceStatus status = ServiceStatus.STOPPED;
    private volatile LocalDateTime startTime;
    private volatile String lastError;

    public ServiceProcess(LauncherConfig.ServiceConfig config, LauncherConfig.GlobalConfig globalConfig, int instanceIndex) {
        this.config = config;
        this.globalConfig = globalConfig;
        this.instanceIndex = instanceIndex;
    }

    /**
     * 获取服务实例名称
     */
    public String getInstanceName() {
        if (config.getInstances() > 1) {
            return config.getName() + "-" + instanceIndex;
        }
        return config.getName();
    }

    /**
     * 启动服务
     */
    public CompletableFuture<Boolean> start() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (status == ServiceStatus.RUNNING) {
                    log.warn("服务已在运行: {}", getInstanceName());
                    return true;
                }

                status = ServiceStatus.STARTING;
                log.info("正在启动服务: {}", getInstanceName());

                // 构建命令
                List<String> command = buildCommand();
                log.debug("启动命令: {}", String.join(" ", command));

                // 创建进程
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(globalConfig.getProjectRoot(), config.getModule()));

                // 设置环境变量
                Map<String, String> env = pb.environment();
                env.putAll(globalConfig.getEnv());
                env.putAll(config.getEnv());

                // 设置端口环境变量
                int actualPort = config.getPort() + instanceIndex;
                int actualDubboPort = config.getDubboPort() > 0 ? config.getDubboPort() + instanceIndex : 0;
                env.put("SERVER_PORT", String.valueOf(actualPort));
                if (actualDubboPort > 0) {
                    env.put("DUBBO_PORT", String.valueOf(actualDubboPort));
                }
                if (config.getProfiles() != null) {
                    env.put("SPRING_PROFILES_ACTIVE", config.getProfiles());
                }

                // 重定向输出到日志文件
                Path logDir = Paths.get(globalConfig.getProjectRoot(), globalConfig.getLogDir());
                Files.createDirectories(logDir);
                String logFileName = getInstanceName() + ".log";
                File logFile = logDir.resolve(logFileName).toFile();
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
                pb.redirectErrorStream(true);

                // 启动进程
                process = pb.start();
                pid = process.pid();
                startTime = LocalDateTime.now();

                // 保存 PID 文件
                savePidFile();

                // 等待启动完成
                boolean started = waitForStartup();
                if (started) {
                    status = ServiceStatus.RUNNING;
                    log.info("服务启动成功: {} (PID: {}, Port: {})", getInstanceName(), pid, actualPort);
                    return true;
                } else {
                    status = ServiceStatus.FAILED;
                    lastError = "启动超时";
                    log.error("服务启动失败: {} - 启动超时", getInstanceName());
                    return false;
                }
            } catch (Exception e) {
                status = ServiceStatus.FAILED;
                lastError = e.getMessage();
                log.error("服务启动失败: {}", getInstanceName(), e);
                return false;
            }
        });
    }

    /**
     * 停止服务
     */
    public CompletableFuture<Boolean> stop() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (status == ServiceStatus.STOPPED) {
                    log.warn("服务未运行: {}", getInstanceName());
                    return true;
                }

                status = ServiceStatus.STOPPING;
                log.info("正在停止服务: {} (PID: {})", getInstanceName(), pid);

                if (process != null && process.isAlive()) {
                    // 优雅停止
                    process.destroy();

                    // 等待进程结束
                    boolean terminated = process.waitFor(globalConfig.getShutdownTimeout(), TimeUnit.SECONDS);
                    if (!terminated) {
                        log.warn("服务未能在 {}s 内停止，强制终止: {}", globalConfig.getShutdownTimeout(), getInstanceName());
                        process.destroyForcibly();
                        process.waitFor(5, TimeUnit.SECONDS);
                    }
                }

                // 删除 PID 文件
                deletePidFile();

                status = ServiceStatus.STOPPED;
                process = null;
                pid = 0;
                log.info("服务已停止: {}", getInstanceName());
                return true;
            } catch (Exception e) {
                status = ServiceStatus.FAILED;
                lastError = e.getMessage();
                log.error("停止服务失败: {}", getInstanceName(), e);
                return false;
            }
        });
    }

    /**
     * 重启服务
     */
    public CompletableFuture<Boolean> restart() {
        return stop().thenCompose(stopped -> {
            if (stopped) {
                return start();
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    /**
     * 检查健康状态
     */
    public boolean checkHealth() {
        if (process == null || !process.isAlive()) {
            status = ServiceStatus.STOPPED;
            return false;
        }

        // 如果配置了健康检查 URL
        String healthUrl = config.getHealthCheckUrl();
        if (healthUrl != null && !healthUrl.isEmpty()) {
            try {
                int actualPort = config.getPort() + instanceIndex;
                String url = healthUrl.replace("${port}", String.valueOf(actualPort));
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                log.debug("健康检查失败: {} - {}", getInstanceName(), e.getMessage());
                return false;
            }
        }

        return process.isAlive();
    }

    /**
     * 构建启动命令
     */
    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();

        // Java 路径
        String javaPath = "java";
        if (globalConfig.getJavaHome() != null && !globalConfig.getJavaHome().isEmpty()) {
            javaPath = Paths.get(globalConfig.getJavaHome(), "bin", "java").toString();
        }
        command.add(javaPath);

        // JVM 参数
        String jvmArgs = config.getJvmArgs() != null ? config.getJvmArgs() : globalConfig.getJvmArgs();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            for (String arg : jvmArgs.split("\\s+")) {
                command.add(arg);
            }
        }

        // 启动方式
        if (config.getMainClass() != null && !config.getMainClass().isEmpty()) {
            command.add("-cp");
            command.add("target/classes:target/dependency/*");
            command.add(config.getMainClass());
        } else {
            command.add("-jar");
            command.add(config.getJar());
        }

        // 应用参数
        if (config.getAppArgs() != null && !config.getAppArgs().isEmpty()) {
            for (String arg : config.getAppArgs().split("\\s+")) {
                command.add(arg);
            }
        }

        return command;
    }

    /**
     * 等待启动完成
     */
    private boolean waitForStartup() {
        int timeout = globalConfig.getStartupTimeout();
        int interval = config.getHealthCheckInterval();

        for (int i = 0; i < timeout / interval; i++) {
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            // 检查进程是否还在运行
            if (process == null || !process.isAlive()) {
                return false;
            }

            // 如果有健康检查 URL，等待健康检查通过
            if (config.getHealthCheckUrl() != null && !config.getHealthCheckUrl().isEmpty()) {
                if (checkHealth()) {
                    return true;
                }
            } else {
                // 没有健康检查 URL，等待几秒后认为启动成功
                if (i >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 保存 PID 文件
     */
    private void savePidFile() throws IOException {
        Path pidDir = Paths.get(globalConfig.getProjectRoot(), globalConfig.getPidDir());
        Files.createDirectories(pidDir);
        Path pidFile = pidDir.resolve(getInstanceName() + ".pid");
        Files.writeString(pidFile, String.valueOf(pid));
    }

    /**
     * 删除 PID 文件
     */
    private void deletePidFile() {
        try {
            Path pidFile = Paths.get(globalConfig.getProjectRoot(), globalConfig.getPidDir(), getInstanceName() + ".pid");
            Files.deleteIfExists(pidFile);
        } catch (IOException e) {
            log.warn("删除 PID 文件失败: {}", getInstanceName(), e);
        }
    }

    /**
     * 获取运行时长
     */
    public String getUptime() {
        if (startTime == null) {
            return "-";
        }
        java.time.Duration duration = java.time.Duration.between(startTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * 服务状态枚举
     */
    public enum ServiceStatus {
        STOPPED("已停止"),
        STARTING("启动中"),
        RUNNING("运行中"),
        STOPPING("停止中"),
        FAILED("失败");

        private final String description;

        ServiceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
