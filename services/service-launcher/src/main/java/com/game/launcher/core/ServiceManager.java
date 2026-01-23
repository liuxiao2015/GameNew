package com.game.launcher.core;

import com.game.launcher.config.LauncherConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务管理器
 *
 * @author GameServer
 */
@Slf4j
public class ServiceManager {

    private final LauncherConfig config;
    private final Map<String, ServiceProcess> processes = new ConcurrentHashMap<>();

    public ServiceManager(LauncherConfig config) {
        this.config = config;
        initializeProcesses();
    }

    /**
     * 初始化服务进程
     */
    private void initializeProcesses() {
        for (LauncherConfig.ServiceConfig serviceConfig : config.getServices()) {
            if (!serviceConfig.isEnabled()) {
                continue;
            }

            for (int i = 0; i < serviceConfig.getInstances(); i++) {
                ServiceProcess process = new ServiceProcess(serviceConfig, config.getGlobal(), i);
                processes.put(process.getInstanceName(), process);
            }
        }
        log.info("初始化 {} 个服务实例", processes.size());
    }

    /**
     * 启动所有服务
     */
    public CompletableFuture<Void> startAll() {
        log.info("========================================");
        log.info("           启动所有服务");
        log.info("========================================");

        // 按启动顺序分组
        Map<Integer, List<ServiceProcess>> orderedProcesses = processes.values().stream()
                .collect(Collectors.groupingBy(p -> p.getConfig().getOrder(), TreeMap::new, Collectors.toList()));

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (Map.Entry<Integer, List<ServiceProcess>> entry : orderedProcesses.entrySet()) {
            List<ServiceProcess> processGroup = entry.getValue();
            future = future.thenCompose(v -> startGroup(processGroup));
        }

        return future.thenRun(() -> {
            log.info("========================================");
            log.info("           所有服务启动完成");
            log.info("========================================");
            printStatus();
        });
    }

    /**
     * 启动一组服务
     */
    private CompletableFuture<Void> startGroup(List<ServiceProcess> processGroup) {
        List<CompletableFuture<Boolean>> futures = processGroup.stream()
                .map(ServiceProcess::start)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 停止所有服务
     */
    public CompletableFuture<Void> stopAll() {
        log.info("========================================");
        log.info("           停止所有服务");
        log.info("========================================");

        // 按启动顺序逆序停止
        Map<Integer, List<ServiceProcess>> orderedProcesses = processes.values().stream()
                .collect(Collectors.groupingBy(
                        (ServiceProcess p) -> p.getConfig().getOrder(), 
                        () -> new TreeMap<Integer, List<ServiceProcess>>(Comparator.reverseOrder()), 
                        Collectors.toList()));

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (Map.Entry<Integer, List<ServiceProcess>> entry : orderedProcesses.entrySet()) {
            List<ServiceProcess> processGroup = entry.getValue();
            future = future.thenCompose(v -> stopGroup(processGroup));
        }

        return future.thenRun(() -> {
            log.info("========================================");
            log.info("           所有服务已停止");
            log.info("========================================");
        });
    }

    /**
     * 停止一组服务
     */
    private CompletableFuture<Void> stopGroup(List<ServiceProcess> processGroup) {
        List<CompletableFuture<Boolean>> futures = processGroup.stream()
                .map(ServiceProcess::stop)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 启动指定服务
     */
    public CompletableFuture<Boolean> startService(String serviceName) {
        ServiceProcess process = processes.get(serviceName);
        if (process == null) {
            log.error("服务不存在: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }
        return process.start();
    }

    /**
     * 停止指定服务
     */
    public CompletableFuture<Boolean> stopService(String serviceName) {
        ServiceProcess process = processes.get(serviceName);
        if (process == null) {
            log.error("服务不存在: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }
        return process.stop();
    }

    /**
     * 重启指定服务
     */
    public CompletableFuture<Boolean> restartService(String serviceName) {
        ServiceProcess process = processes.get(serviceName);
        if (process == null) {
            log.error("服务不存在: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }
        return process.restart();
    }

    /**
     * 重启所有服务
     */
    public CompletableFuture<Void> restartAll() {
        return stopAll().thenCompose(v -> startAll());
    }

    /**
     * 启动指定组的服务
     */
    public CompletableFuture<Void> startGroup(String groupName) {
        LauncherConfig.ServiceGroup group = config.getGroups().get(groupName);
        if (group == null) {
            log.error("服务组不存在: {}", groupName);
            return CompletableFuture.completedFuture(null);
        }

        log.info("启动服务组: {}", groupName);
        List<ServiceProcess> groupProcesses = processes.values().stream()
                .filter(p -> group.getServices().contains(p.getConfig().getName()))
                .collect(Collectors.toList());

        return startGroup(groupProcesses);
    }

    /**
     * 停止指定组的服务
     */
    public CompletableFuture<Void> stopGroup(String groupName) {
        LauncherConfig.ServiceGroup group = config.getGroups().get(groupName);
        if (group == null) {
            log.error("服务组不存在: {}", groupName);
            return CompletableFuture.completedFuture(null);
        }

        log.info("停止服务组: {}", groupName);
        List<ServiceProcess> groupProcesses = processes.values().stream()
                .filter(p -> group.getServices().contains(p.getConfig().getName()))
                .collect(Collectors.toList());

        return stopGroup(groupProcesses);
    }

    /**
     * 获取服务状态
     */
    public ServiceProcess getProcess(String serviceName) {
        return processes.get(serviceName);
    }

    /**
     * 获取所有服务
     */
    public Collection<ServiceProcess> getAllProcesses() {
        return processes.values();
    }

    /**
     * 打印服务状态
     */
    public void printStatus() {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                         服务状态                                │");
        System.out.println("├──────────────────────┬──────────┬────────┬──────────┬──────────┤");
        System.out.println("│ 服务名称             │ 状态     │ PID    │ 端口     │ 运行时长 │");
        System.out.println("├──────────────────────┼──────────┼────────┼──────────┼──────────┤");

        for (ServiceProcess process : processes.values()) {
            String name = padRight(process.getInstanceName(), 20);
            String status = padRight(process.getStatus().getDescription(), 8);
            String pid = padRight(String.valueOf(process.getPid()), 6);
            int port = process.getConfig().getPort() + process.getInstanceIndex();
            String portStr = padRight(String.valueOf(port), 8);
            String uptime = padRight(process.getUptime(), 8);

            System.out.printf("│ %s │ %s │ %s │ %s │ %s │%n", 
                    name, status, pid, portStr, uptime);
        }

        System.out.println("└──────────────────────┴──────────┴────────┴──────────┴──────────┘");
        System.out.println();
    }

    /**
     * 右填充字符串
     */
    private String padRight(String str, int length) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * 获取运行中的服务数量
     */
    public long getRunningCount() {
        return processes.values().stream()
                .filter(p -> p.getStatus() == ServiceProcess.ServiceStatus.RUNNING)
                .count();
    }

    /**
     * 获取服务总数
     */
    public int getTotalCount() {
        return processes.size();
    }
}
