package com.game.launcher;

import com.game.launcher.command.CommandHandler;
import com.game.launcher.config.ConfigLoader;
import com.game.launcher.config.LauncherConfig;
import com.game.launcher.core.ServiceManager;
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

        try {
            handler.execute(command, commandArgs != null ? commandArgs : new String[0]);
        } catch (Exception e) {
            log.error("执行命令失败: {}", command, e);
            System.exit(1);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        printBanner();

        LauncherConfig config = ConfigLoader.load(configPath);
        ServiceManager manager = new ServiceManager(config);
        CommandHandler handler = new CommandHandler(manager);

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭所有服务...");
            manager.stopAll().join();
        }));

        // 交互模式
        System.out.println("输入 'help' 查看可用命令");
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
