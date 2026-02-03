package com.game.launcher.infra;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker 基础设施管理器
 * <p>
 * 管理 Docker Compose 容器的启动、停止和状态查询
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class DockerManager {

    private final Path dockerComposePath;
    private final Path projectRoot;

    public DockerManager(String projectRoot) {
        this.projectRoot = Paths.get(projectRoot);
        this.dockerComposePath = this.projectRoot.resolve("docker/docker-compose.yml");
    }

    /**
     * 检查 Docker 是否可用
     */
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Docker 不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Docker Compose 文件是否存在
     */
    public boolean hasDockerCompose() {
        return Files.exists(dockerComposePath);
    }

    /**
     * 启动所有基础设施容器
     */
    public boolean startAll() {
        return startContainers(null);
    }

    /**
     * 启动指定的容器
     */
    public boolean startContainers(List<String> services) {
        log.info("========================================");
        log.info("        启动 Docker 基础设施");
        log.info("========================================");

        if (!isDockerAvailable()) {
            log.error("Docker 未安装或未运行，请先启动 Docker");
            return false;
        }

        if (!hasDockerCompose()) {
            log.error("docker-compose.yml 文件不存在: {}", dockerComposePath);
            return false;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add("docker-compose");
            command.add("-f");
            command.add(dockerComposePath.toString());
            command.add("up");
            command.add("-d");

            if (services != null && !services.isEmpty()) {
                command.addAll(services);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectRoot.resolve("docker").toFile());
            pb.redirectErrorStream(true);

            log.info("执行: {}", String.join(" ", command));

            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("  {}", line);
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Docker Compose 启动超时");
                process.destroyForcibly();
                return false;
            }

            if (process.exitValue() == 0) {
                log.info("========================================");
                log.info("        Docker 基础设施启动成功");
                log.info("========================================");
                return true;
            } else {
                log.error("Docker Compose 启动失败，退出码: {}", process.exitValue());
                return false;
            }
        } catch (Exception e) {
            log.error("启动 Docker 基础设施失败", e);
            return false;
        }
    }

    /**
     * 停止所有基础设施容器
     */
    public boolean stopAll() {
        return stopContainers(null);
    }

    /**
     * 停止指定的容器
     */
    public boolean stopContainers(List<String> services) {
        log.info("停止 Docker 基础设施...");

        if (!isDockerAvailable()) {
            log.error("Docker 不可用");
            return false;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add("docker-compose");
            command.add("-f");
            command.add(dockerComposePath.toString());
            command.add("stop");

            if (services != null && !services.isEmpty()) {
                command.addAll(services);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectRoot.resolve("docker").toFile());
            pb.inheritIO();

            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.error("停止 Docker 基础设施失败", e);
            return false;
        }
    }

    /**
     * 获取容器状态
     */
    public List<ContainerStatus> getStatus() {
        List<ContainerStatus> statusList = new ArrayList<>();

        if (!isDockerAvailable()) {
            return statusList;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose",
                    "-f", dockerComposePath.toString(),
                    "ps", "--format", "{{.Name}}|{{.Status}}|{{.Ports}}"
            );
            pb.directory(projectRoot.resolve("docker").toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        String status = parts[1].trim();
                        String ports = parts.length > 2 ? parts[2].trim() : "";
                        boolean running = status.toLowerCase().contains("up");
                        statusList.add(new ContainerStatus(name, running, status, ports));
                    }
                }
            }

            process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取容器状态失败", e);
        }

        return statusList;
    }

    /**
     * 打印容器状态
     */
    public void printStatus() {
        System.out.println();
        System.out.println("┌────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                           Docker 容器状态                                  │");
        System.out.println("├────────────────────┬────────────────────┬──────────────────────────────────┤");
        System.out.println("│ 容器名称           │ 状态               │ 端口                             │");
        System.out.println("├────────────────────┼────────────────────┼──────────────────────────────────┤");

        List<ContainerStatus> statusList = getStatus();
        if (statusList.isEmpty()) {
            System.out.println("│ (无运行中的容器)                                                           │");
        } else {
            for (ContainerStatus status : statusList) {
                String name = padRight(status.name(), 18);
                String state = padRight(status.running() ? "✓ " + truncate(status.status(), 16) : "✗ 已停止", 18);
                String ports = padRight(truncate(status.ports(), 32), 32);
                System.out.printf("│ %s │ %s │ %s │%n", name, state, ports);
            }
        }

        System.out.println("└────────────────────┴────────────────────┴──────────────────────────────────┘");
        System.out.println();
    }

    /**
     * 查看容器日志
     */
    public void showLogs(String containerName, int lines) {
        if (!isDockerAvailable()) {
            System.out.println("Docker 不可用");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose",
                    "-f", dockerComposePath.toString(),
                    "logs", "--tail", String.valueOf(lines), containerName
            );
            pb.directory(projectRoot.resolve("docker").toFile());
            pb.inheritIO();

            Process process = pb.start();
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("查看日志失败", e);
        }
    }

    /**
     * 重启容器
     */
    public boolean restartContainer(String containerName) {
        log.info("重启容器: {}", containerName);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose",
                    "-f", dockerComposePath.toString(),
                    "restart", containerName
            );
            pb.directory(projectRoot.resolve("docker").toFile());
            pb.inheritIO();

            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.error("重启容器失败", e);
            return false;
        }
    }

    private String padRight(String str, int length) {
        if (str == null) str = "";
        int actualLength = 0;
        for (char c : str.toCharArray()) {
            actualLength += (c > 127 ? 2 : 1);
        }
        if (actualLength >= length) return str.substring(0, Math.min(str.length(), length));
        return str + " ".repeat(length - actualLength);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 容器状态
     */
    public record ContainerStatus(String name, boolean running, String status, String ports) {
    }
}
