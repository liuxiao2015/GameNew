package com.game.launcher;

import com.game.launcher.command.CommandHandler;
import com.game.launcher.config.ConfigLoader;
import com.game.launcher.config.LauncherConfig;
import com.game.launcher.core.ServiceManager;
import com.game.launcher.infra.NacosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

/**
 * 服务启动器应用
 * <p>
 * 通过配置文件一键启动/停止多个微服务
 * </p>
 * <p>
 * IDEA 运行方式:
 * 1. 直接运行 main 方法，进入交互模式
 * 2. 配置 Program Arguments: up --local (一键本地启动)
 * 3. 配置 Program Arguments: up (Docker 模式)
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@SpringBootApplication
public class LauncherApplication implements CommandLineRunner {

    private static String configPath = null;
    private static String command = null;
    private static String[] commandArgs = null;

    public static void main(String[] args) {
        // 解析命令行参数
        parseArgs(args);

        if (command == null) {
            // 交互模式
            SpringApplication.run(LauncherApplication.class, args);
        } else {
            // 命令模式
            executeCommand();
        }
    }

    /**
     * 解析命令行参数
     */
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--config=")) {
                configPath = args[i].substring("--config=".length());
            } else if (args[i].startsWith("-c")) {
                if (i + 1 < args.length) {
                    configPath = args[++i];
                }
            } else if (!args[i].startsWith("-")) {
                command = args[i];
                // 收集剩余参数
                if (i + 1 < args.length) {
                    commandArgs = new String[args.length - i - 1];
                    System.arraycopy(args, i + 1, commandArgs, 0, commandArgs.length);
                }
                break;
            }
        }
    }

    /**
     * 执行命令模式
     */
    private static void executeCommand() {
        LauncherConfig config = ConfigLoader.load(configPath);
        ServiceManager manager = new ServiceManager(config);
        CommandHandler handler = new CommandHandler(manager);

        // 注册关闭钩子 (确保服务正常关闭)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭所有服务...");
            manager.stopAll().join();
        }));

        try {
            handler.execute(command, commandArgs != null ? commandArgs : new String[0]);

            // 如果是启动命令，保持进程运行
            if (isStartCommand(command)) {
                System.out.println();
                System.out.println("服务已启动，按 Ctrl+C 停止所有服务");
                System.out.println("或输入命令继续操作 (help 查看帮助):");
                System.out.println();

                // 进入简单的交互模式
                try (Scanner scanner = new Scanner(System.in)) {
                    while (true) {
                        System.out.print("launcher> ");
                        if (!scanner.hasNextLine()) {
                            // 等待输入
                            Thread.sleep(1000);
                            continue;
                        }
                        String line = scanner.nextLine().trim();
                        if (line.isEmpty()) continue;

                        if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                            System.out.println("正在退出...");
                            break;
                        }

                        String[] parts = line.split("\\s+");
                        String cmd = parts[0];
                        String[] cmdArgs = new String[parts.length - 1];
                        System.arraycopy(parts, 1, cmdArgs, 0, cmdArgs.length);
                        handler.execute(cmd, cmdArgs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}", command, e);
            System.exit(1);
        }
    }

    /**
     * 判断是否为启动命令
     */
    private static boolean isStartCommand(String cmd) {
        return "up".equalsIgnoreCase(cmd) || "start".equalsIgnoreCase(cmd);
    }

    @Override
    public void run(String... args) throws Exception {
        printBanner();

        LauncherConfig config = ConfigLoader.load(configPath);
        ServiceManager manager = new ServiceManager(config);
        CommandHandler handler = new CommandHandler(manager);
        NacosManager nacosManager = new NacosManager(config.getGlobal().getProjectRoot());

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭所有服务...");
            manager.stopAll().join();
        }));

        // 显示启动菜单
        showStartupMenu(handler, nacosManager);

        // 交互模式
        System.out.println();
        System.out.println("输入 'help' 查看可用命令, 'exit' 退出");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("launcher> ");
                String line = scanner.nextLine().trim();

                if (line.isEmpty()) {
                    continue;
                }

                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    System.out.println("正在退出...");
                    manager.stopAll().join();
                    break;
                }

                try {
                    String[] parts = line.split("\\s+");
                    String cmd = parts[0];
                    String[] cmdArgs = new String[parts.length - 1];
                    System.arraycopy(parts, 1, cmdArgs, 0, cmdArgs.length);
                    handler.execute(cmd, cmdArgs);
                } catch (Exception e) {
                    System.err.println("错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 显示启动菜单
     */
    private void showStartupMenu(CommandHandler handler, NacosManager nacosManager) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      请选择启动模式                            ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  [1] 一键启动 (本地模式) - 自动启动 Nacos，适合 Windows 开发   ║");
        System.out.println("║  [2] 一键启动 (Docker 模式) - 需要 Docker Desktop              ║");
        System.out.println("║  [3] 仅启动 Nacos                                             ║");
        System.out.println("║  [4] 仅启动游戏服务 (Nacos 已运行)                             ║");
        System.out.println("║  [5] 查看状态                                                  ║");
        System.out.println("║  [0] 跳过，进入交互模式                                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 显示当前状态
        boolean nacosRunning = nacosManager.isRunning();
        System.out.println("当前状态:");
        System.out.println("  Nacos: " + (nacosRunning ? "✓ 运行中" : "✗ 未运行"));
        System.out.println();

        System.out.print("请选择 [0-5]: ");

        try (Scanner menuScanner = new Scanner(System.in)) {
            String choice = menuScanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println();
                    handler.execute("up", new String[]{"--local"});
                }
                case "2" -> {
                    System.out.println();
                    handler.execute("up", new String[]{});
                }
                case "3" -> {
                    System.out.println();
                    handler.execute("nacos", new String[]{"start"});
                }
                case "4" -> {
                    System.out.println();
                    handler.execute("start", new String[]{"all"});
                }
                case "5" -> {
                    System.out.println();
                    handler.execute("status", new String[]{});
                }
                default -> System.out.println("进入交互模式...");
            }
        } catch (Exception e) {
            System.out.println("进入交互模式...");
        }
    }

    /**
     * 打印 Banner
     */
    private void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                               ║");
        System.out.println("║   ██████╗  █████╗ ███╗   ███╗███████╗                        ║");
        System.out.println("║  ██╔════╝ ██╔══██╗████╗ ████║██╔════╝                        ║");
        System.out.println("║  ██║  ███╗███████║██╔████╔██║█████╗                          ║");
        System.out.println("║  ██║   ██║██╔══██║██║╚██╔╝██║██╔══╝                          ║");
        System.out.println("║  ╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗                        ║");
        System.out.println("║   ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝                        ║");
        System.out.println("║                                                               ║");
        System.out.println("║              服务启动器 v1.0.0                                ║");
        System.out.println("║                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
