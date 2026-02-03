package com.game.launcher.command;

import com.game.launcher.core.ServiceManager;
import com.game.launcher.core.ServiceProcess;
import com.game.launcher.infra.DockerManager;
import com.game.launcher.infra.InfrastructureChecker;
import com.game.launcher.infra.NacosManager;
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
public class CommandHandler {

    private final ServiceManager manager;
    private final DockerManager dockerManager;
    private final NacosManager nacosManager;
    private final InfrastructureChecker infraChecker;
    private final Map<String, BiConsumer<String, String[]>> commands = new HashMap<>();

    public CommandHandler(ServiceManager manager) {
        this.manager = manager;
        String projectRoot = manager.getConfig().getGlobal().getProjectRoot();
        this.dockerManager = new DockerManager(projectRoot);
        this.nacosManager = new NacosManager(projectRoot);
        this.infraChecker = new InfrastructureChecker();

        // 注册命令
        commands.put("start", this::handleStart);
        commands.put("stop", this::handleStop);
        commands.put("restart", this::handleRestart);
        commands.put("status", this::handleStatus);
        commands.put("list", this::handleList);
        commands.put("help", this::handleHelp);
        commands.put("logs", this::handleLogs);

        // Docker 相关命令
        commands.put("docker", this::handleDocker);
        commands.put("infra", this::handleInfra);
        commands.put("check", this::handleCheck);

        // Nacos 本地管理
        commands.put("nacos", this::handleNacos);

        // 快捷命令
        commands.put("up", this::handleUp);
        commands.put("down", this::handleDown);
        commands.put("refresh", this::handleRefresh);
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
     * 处理 up 命令 - 一键启动所有 (Docker/本地 Nacos + 服务)
     * 参数:
     *   -f, --force    跳过基础设施检查，强制启动服务
     *   --no-docker    不尝试启动 Docker，使用本地 Nacos
     *   --local        同 --no-docker，使用本地基础设施
     */
    private void handleUp(String cmd, String[] args) {
        boolean skipCheck = false;
        boolean localMode = false;

        // 解析参数
        for (String arg : args) {
            if ("-f".equals(arg) || "--force".equals(arg) || "--skip-check".equals(arg)) {
                skipCheck = true;
            } else if ("--no-docker".equals(arg) || "--local".equals(arg)) {
                localMode = true;
            }
        }

        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      一键启动游戏服务器                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 检测运行模式
        boolean dockerAvailable = dockerManager.isDockerAvailable();
        boolean useDocker = !localMode && dockerAvailable;

        if (useDocker) {
            // Docker 模式
            handleUpWithDocker(skipCheck);
        } else {
            // 本地模式 (Windows 本地开发推荐)
            handleUpWithLocal(skipCheck);
        }
    }

    /**
     * Docker 模式启动
     */
    private void handleUpWithDocker(boolean skipCheck) {
        // 1. 启动 Docker 基础设施
        if (!infraChecker.quickCheck()) {
            System.out.println("[1/3] 启动 Docker 基础设施...");
            dockerManager.startAll();
        } else {
            System.out.println("[1/3] Docker 基础设施已运行");
        }

        // 2. 等待基础设施就绪
        if (!skipCheck) {
            System.out.println("[2/3] 等待基础设施就绪...");
            boolean ready = infraChecker.waitForEssential();
            if (!ready) {
                printInfraError();
                return;
            }
        } else {
            System.out.println("[2/3] 跳过基础设施检查 (--force 模式)");
        }

        // 3. 启动所有服务
        startAllServices();
    }

    /**
     * 本地模式启动 (Windows 本地开发)
     */
    private void handleUpWithLocal(boolean skipCheck) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      本地开发模式                              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 1. 检查并启动本地 Nacos
        System.out.println("[1/4] 检查本地 Nacos...");
        if (!nacosManager.isRunning()) {
            System.out.println("      Nacos 未运行，正在启动...");
            if (!nacosManager.start()) {
                System.out.println();
                System.out.println("╔═══════════════════════════════════════════════════════════════╗");
                System.out.println("║  ✗ Nacos 启动失败                                             ║");
                System.out.println("╠═══════════════════════════════════════════════════════════════╣");
                System.out.println("║  请检查:                                                       ║");
                System.out.println("║  1. tools/nacos-server-2.3.0.zip 是否存在                     ║");
                System.out.println("║  2. 端口 8848, 9848 是否被占用                                ║");
                System.out.println("║  3. 查看日志: logs/nacos-startup.log                          ║");
                System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                return;
            }
        } else {
            System.out.println("      ✓ Nacos 已运行");
        }

        // 2. 检查其他基础设施
        System.out.println("[2/4] 检查其他基础设施...");
        boolean mongoReady = checkPortQuiet(27017);
        boolean redisReady = checkPortQuiet(6379);

        if (!mongoReady || !redisReady) {
            System.out.println();
            System.out.println("┌─────────────────────────────────────────────────────────────────┐");
            System.out.println("│  其他基础设施状态:                                               │");
            System.out.printf("│    MongoDB (27017): %-44s │%n", mongoReady ? "✓ 已就绪" : "✗ 未就绪 - 需要手动启动");
            System.out.printf("│    Redis   (6379) : %-44s │%n", redisReady ? "✓ 已就绪" : "✗ 未就绪 - 需要手动启动");
            System.out.println("├─────────────────────────────────────────────────────────────────┤");
            System.out.println("│  提示: 可以下载并运行以下软件:                                   │");
            System.out.println("│  • MongoDB: https://www.mongodb.com/try/download/community      │");
            System.out.println("│  • Redis  : https://github.com/tporadowski/redis/releases       │");
            System.out.println("└─────────────────────────────────────────────────────────────────┘");
            System.out.println();

            if (!skipCheck) {
                System.out.println("使用 'up -f --local' 可跳过检查强制启动");
                return;
            }
        } else {
            System.out.println("      ✓ MongoDB 已就绪");
            System.out.println("      ✓ Redis 已就绪");
        }

        // 3. 等待 Nacos 完全就绪
        if (!skipCheck) {
            System.out.println("[3/4] 等待服务注册中心就绪...");
            if (!nacosManager.waitForReady(30)) {
                System.out.println("Nacos 未完全就绪，但将尝试继续启动...");
            }
        } else {
            System.out.println("[3/4] 跳过就绪检查 (--force 模式)");
        }

        // 4. 启动所有服务
        System.out.println("[4/4] 启动游戏服务...");
        startAllServices();
    }

    /**
     * 启动所有服务并显示结果
     */
    private void startAllServices() {
        manager.startAll().join();

        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      启动完成！                                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        manager.printStatus();
    }

    /**
     * 打印基础设施错误
     */
    private void printInfraError() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ✗ 基础设施未就绪，无法启动服务                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  解决方案:                                                     ║");
        System.out.println("║  1. 使用 'up --local' 启动本地 Nacos (推荐 Windows)           ║");
        System.out.println("║  2. 安装并启动 Docker Desktop，然后运行 'up'                  ║");
        System.out.println("║  3. 手动启动基础设施后运行 'up -f' 跳过检查                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }

    /**
     * 静默检查端口
     */
    private boolean checkPortQuiet(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
     * 处理 nacos 命令 - 管理本地 Nacos
     */
    private void handleNacos(String cmd, String[] args) {
        if (args.length == 0) {
            nacosManager.printStatus();
            return;
        }

        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "start" -> {
                System.out.println("启动本地 Nacos...");
                if (nacosManager.start()) {
                    System.out.println("Nacos 启动成功！");
                } else {
                    System.out.println("Nacos 启动失败，请检查日志");
                }
            }
            case "stop" -> {
                System.out.println("停止本地 Nacos...");
                if (nacosManager.stop()) {
                    System.out.println("Nacos 已停止");
                } else {
                    System.out.println("Nacos 停止失败");
                }
            }
            case "restart" -> {
                System.out.println("重启本地 Nacos...");
                nacosManager.stop();
                if (nacosManager.start()) {
                    System.out.println("Nacos 重启成功！");
                } else {
                    System.out.println("Nacos 重启失败");
                }
            }
            case "install" -> {
                System.out.println("安装本地 Nacos...");
                if (nacosManager.install()) {
                    System.out.println("Nacos 安装完成！");
                    System.out.println("安装路径: " + nacosManager.getNacosHome());
                } else {
                    System.out.println("Nacos 安装失败");
                }
            }
            case "status" -> nacosManager.printStatus();
            default -> {
                System.out.println("未知 nacos 子命令: " + subCmd);
                System.out.println("可用命令: start, stop, restart, install, status");
            }
        }
    }

    /**
     * 处理 down 命令 - 一键停止所有
     */
    private void handleDown(String cmd, String[] args) {
        System.out.println();
        System.out.println("停止所有服务...");

        // 先停止服务
        manager.stopAll().join();

        // 可选: 停止基础设施
        if (args.length > 0 && "all".equalsIgnoreCase(args[0])) {
            // 停止本地 Nacos
            if (nacosManager.isRunning()) {
                System.out.println("停止本地 Nacos...");
                nacosManager.stop();
            }
            // 停止 Docker
            if (dockerManager.isDockerAvailable()) {
                System.out.println("停止 Docker 基础设施...");
                dockerManager.stopAll();
            }
        }

        System.out.println();
        System.out.println("所有服务已停止");
    }

    /**
     * 处理 start 命令
     */
    private void handleStart(String cmd, String[] args) {
        // 先检查基础设施
        if (!infraChecker.quickCheck()) {
            System.out.println("警告: 基础设施未完全就绪，建议先执行 'up' 或 'infra start'");
            System.out.print("是否继续启动? (y/n): ");
            // 在非交互模式下直接继续
        }

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
            infraChecker.printStatus();
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
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.printf("  %-20s %-15s %-8s %s%n", "服务名称", "描述", "端口", "状态");
        System.out.println("─────────────────────────────────────────────────────────────────");

        for (ServiceProcess process : manager.getAllProcesses()) {
            String status = process.getStatus() == ServiceProcess.ServiceStatus.RUNNING ? "● 运行中" : "○ 已停止";
            String desc = process.getConfig().getDescription();
            if (desc == null || desc.isEmpty()) {
                desc = "-";
            }
            System.out.printf("  %-20s %-15s %-8d %s%n",
                    process.getInstanceName(),
                    truncate(desc, 13),
                    process.getConfig().getPort(),
                    status);
        }

        System.out.println();
        System.out.printf("总计: %d 个服务, %d 个运行中%n",
                manager.getTotalCount(),
                manager.getRunningCount());
        System.out.println();

        // 显示服务组
        if (!manager.getConfig().getGroups().isEmpty()) {
            System.out.println("服务组:");
            System.out.println("─────────────────────────────────────────────────────────────────");
            for (var group : manager.getConfig().getGroups().values()) {
                System.out.printf("  %-12s %s (%d 个服务)%n",
                        group.getName(),
                        group.getDescription(),
                        group.getServices().size());
            }
            System.out.println();
        }
    }

    /**
     * 处理 logs 命令
     */
    private void handleLogs(String cmd, String[] args) {
        if (args.length == 0) {
            System.out.println("用法: logs <服务名|docker:容器名> [行数]");
            return;
        }

        String target = args[0];
        int lines = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        if (target.startsWith("docker:")) {
            String containerName = target.substring("docker:".length());
            dockerManager.showLogs(containerName, lines);
            return;
        }

        ServiceProcess process = manager.getProcess(target);
        if (process == null) {
            System.out.println("服务不存在: " + target);
            return;
        }

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
     * 处理 docker 命令
     */
    private void handleDocker(String cmd, String[] args) {
        if (args.length == 0) {
            dockerManager.printStatus();
            return;
        }

        String subCmd = args[0];
        switch (subCmd.toLowerCase()) {
            case "start", "up" -> dockerManager.startAll();
            case "stop", "down" -> dockerManager.stopAll();
            case "status" -> dockerManager.printStatus();
            case "restart" -> {
                if (args.length > 1) {
                    dockerManager.restartContainer(args[1]);
                } else {
                    dockerManager.stopAll();
                    dockerManager.startAll();
                }
            }
            case "logs" -> {
                if (args.length > 1) {
                    int lines = args.length > 2 ? Integer.parseInt(args[2]) : 50;
                    dockerManager.showLogs(args[1], lines);
                } else {
                    System.out.println("用法: docker logs <容器名> [行数]");
                }
            }
            default -> System.out.println("未知 docker 子命令: " + subCmd);
        }
    }

    /**
     * 处理 infra 命令 (docker 的别名)
     */
    private void handleInfra(String cmd, String[] args) {
        handleDocker(cmd, args);
    }

    /**
     * 处理 check 命令 - 检查基础设施状态
     */
    private void handleCheck(String cmd, String[] args) {
        infraChecker.printStatus();
    }

    /**
     * 处理 refresh 命令 - 刷新服务列表
     */
    private void handleRefresh(String cmd, String[] args) {
        System.out.println("刷新服务列表...");
        manager.refresh();
        System.out.println("发现 " + manager.getTotalCount() + " 个服务");
        handleList(cmd, args);
    }

    /**
     * 处理 help 命令
     */
    private void handleHelp(String cmd, String[] args) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         命令帮助                                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("快捷命令:");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  up                               一键启动 (自动检测 Docker/本地)");
        System.out.println("  up --local                       本地模式 (自动启动 Nacos)");
        System.out.println("  up -f                            跳过基础设施检查，强制启动");
        System.out.println("  down [all]                       停止所有服务 (all=含Docker/Nacos)");
        System.out.println("  check                            检查基础设施状态");
        System.out.println("  refresh                          刷新/发现新服务");
        System.out.println();
        System.out.println("服务管理:");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  start [服务名|all|group:组名]    启动服务");
        System.out.println("  stop [服务名|all|group:组名]     停止服务");
        System.out.println("  restart [服务名|all]             重启服务");
        System.out.println("  status [服务名]                  查看服务状态");
        System.out.println("  list                             列出所有服务");
        System.out.println("  logs <服务名> [行数]             查看服务日志");
        System.out.println();
        System.out.println("Nacos 本地管理 (Windows 推荐):");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  nacos start                      启动本地 Nacos");
        System.out.println("  nacos stop                       停止本地 Nacos");
        System.out.println("  nacos restart                    重启本地 Nacos");
        System.out.println("  nacos install                    安装(解压) Nacos");
        System.out.println("  nacos status                     查看 Nacos 状态");
        System.out.println();
        System.out.println("Docker 管理:");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  docker start                     启动 Docker 基础设施");
        System.out.println("  docker stop                      停止 Docker 基础设施");
        System.out.println("  docker status                    查看 Docker 容器状态");
        System.out.println("  docker restart <容器名>          重启指定容器");
        System.out.println("  docker logs <容器名> [行数]      查看容器日志");
        System.out.println();
        System.out.println("其他:");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  help                             显示帮助");
        System.out.println("  exit/quit                        退出启动器");
        System.out.println();
        System.out.println("示例 (Windows 本地开发):");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  up --local                       一键启动 (自动启动本地 Nacos)");
        System.out.println("  nacos start                      单独启动 Nacos");
        System.out.println("  start all                        启动所有游戏服务");
        System.out.println();
        System.out.println("示例 (Docker 环境):");
        System.out.println("───────────────────────────────────────────────────────────────────");
        System.out.println("  up                               一键启动 (Docker + 所有服务)");
        System.out.println("  docker status                    查看容器状态");
        System.out.println("  logs service-game 100            查看最近100行日志");
        System.out.println();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        // 计算实际显示宽度（中文算2）
        int width = 0;
        int i = 0;
        for (; i < str.length() && width < maxLen - 2; i++) {
            width += (str.charAt(i) > 127 ? 2 : 1);
        }
        if (i < str.length()) {
            return str.substring(0, i) + "..";
        }
        return str;
    }
}
