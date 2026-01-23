package com.game.launcher.command;

import com.game.launcher.core.ServiceManager;
import com.game.launcher.core.ServiceProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 命令处理器
 *
 * @author GameServer
 */
@Slf4j
@RequiredArgsConstructor
public class CommandHandler {

    private final ServiceManager manager;
    private final Map<String, BiConsumer<String, String[]>> commands = new HashMap<>();

    {
        // 注册命令
        commands.put("start", this::handleStart);
        commands.put("stop", this::handleStop);
        commands.put("restart", this::handleRestart);
        commands.put("status", this::handleStatus);
        commands.put("list", this::handleList);
        commands.put("help", this::handleHelp);
        commands.put("logs", this::handleLogs);
    }

    /**
     * 执行命令
     */
    public void execute(String command, String[] args) {
        BiConsumer<String, String[]> handler = commands.get(command.toLowerCase());
        if (handler == null) {
            System.out.println("未知命令: " + command);
            System.out.println("输入 'help' 查看可用命令");
            return;
        }
        handler.accept(command, args);
    }

    /**
     * 处理 start 命令
     */
    private void handleStart(String cmd, String[] args) {
        if (args.length == 0 || "all".equalsIgnoreCase(args[0])) {
            manager.startAll().join();
        } else if (args[0].startsWith("group:")) {
            String groupName = args[0].substring("group:".length());
            manager.startGroup(groupName).join();
        } else {
            for (String serviceName : args) {
                manager.startService(serviceName).join();
            }
        }
    }

    /**
     * 处理 stop 命令
     */
    private void handleStop(String cmd, String[] args) {
        if (args.length == 0 || "all".equalsIgnoreCase(args[0])) {
            manager.stopAll().join();
        } else if (args[0].startsWith("group:")) {
            String groupName = args[0].substring("group:".length());
            manager.stopGroup(groupName).join();
        } else {
            for (String serviceName : args) {
                manager.stopService(serviceName).join();
            }
        }
    }

    /**
     * 处理 restart 命令
     */
    private void handleRestart(String cmd, String[] args) {
        if (args.length == 0 || "all".equalsIgnoreCase(args[0])) {
            manager.restartAll().join();
        } else {
            for (String serviceName : args) {
                manager.restartService(serviceName).join();
            }
        }
    }

    /**
     * 处理 status 命令
     */
    private void handleStatus(String cmd, String[] args) {
        if (args.length == 0) {
            manager.printStatus();
        } else {
            for (String serviceName : args) {
                ServiceProcess process = manager.getProcess(serviceName);
                if (process != null) {
                    System.out.printf("服务: %s%n", process.getInstanceName());
                    System.out.printf("  状态: %s%n", process.getStatus().getDescription());
                    System.out.printf("  PID: %d%n", process.getPid());
                    System.out.printf("  端口: %d%n", process.getConfig().getPort() + process.getInstanceIndex());
                    System.out.printf("  运行时长: %s%n", process.getUptime());
                    if (process.getLastError() != null) {
                        System.out.printf("  最后错误: %s%n", process.getLastError());
                    }
                    System.out.println();
                } else {
                    System.out.println("服务不存在: " + serviceName);
                }
            }
        }
    }

    /**
     * 处理 list 命令
     */
    private void handleList(String cmd, String[] args) {
        System.out.println();
        System.out.println("可用服务列表:");
        System.out.println("─────────────────────────────────────────");
        for (ServiceProcess process : manager.getAllProcesses()) {
            String status = process.getStatus() == ServiceProcess.ServiceStatus.RUNNING ? "●" : "○";
            System.out.printf("  %s %s (%s)%n", 
                    status, 
                    process.getInstanceName(),
                    process.getConfig().getDescription());
        }
        System.out.println();
        System.out.printf("总计: %d 个服务, %d 个运行中%n", 
                manager.getTotalCount(), 
                manager.getRunningCount());
        System.out.println();
    }

    /**
     * 处理 logs 命令
     */
    private void handleLogs(String cmd, String[] args) {
        if (args.length == 0) {
            System.out.println("用法: logs <服务名> [行数]");
            return;
        }

        String serviceName = args[0];
        int lines = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        ServiceProcess process = manager.getProcess(serviceName);
        if (process == null) {
            System.out.println("服务不存在: " + serviceName);
            return;
        }

        // 读取日志文件
        try {
            java.nio.file.Path logFile = java.nio.file.Paths.get(
                    process.getGlobalConfig().getProjectRoot(),
                    process.getGlobalConfig().getLogDir(),
                    process.getInstanceName() + ".log"
            );

            if (!java.nio.file.Files.exists(logFile)) {
                System.out.println("日志文件不存在: " + logFile);
                return;
            }

            java.util.List<String> allLines = java.nio.file.Files.readAllLines(logFile);
            int startIndex = Math.max(0, allLines.size() - lines);
            for (int i = startIndex; i < allLines.size(); i++) {
                System.out.println(allLines.get(i));
            }
        } catch (Exception e) {
            System.out.println("读取日志失败: " + e.getMessage());
        }
    }

    /**
     * 处理 help 命令
     */
    private void handleHelp(String cmd, String[] args) {
        System.out.println();
        System.out.println("可用命令:");
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("  start [服务名|all|group:组名]    启动服务");
        System.out.println("  stop [服务名|all|group:组名]     停止服务");
        System.out.println("  restart [服务名|all]             重启服务");
        System.out.println("  status [服务名]                  查看服务状态");
        System.out.println("  list                             列出所有服务");
        System.out.println("  logs <服务名> [行数]             查看服务日志");
        System.out.println("  help                             显示帮助");
        System.out.println("  exit/quit                        退出启动器");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  start all                        启动所有服务");
        System.out.println("  start service-game               启动游戏服务");
        System.out.println("  start group:core                 启动核心服务组");
        System.out.println("  stop service-chat service-guild  停止多个服务");
        System.out.println("  logs service-game 100            查看最近100行日志");
        System.out.println();
    }
}
