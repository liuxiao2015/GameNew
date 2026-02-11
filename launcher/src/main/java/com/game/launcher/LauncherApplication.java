package com.game.launcher;

import com.game.common.launcher.GameService;
import com.game.launcher.infra.EmbeddedNacos;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 游戏服务一键启动器
 * <p>
 * 在 IDEA 中直接运行此 main 方法, 即可自动发现并启动所有标记了
 * {@link GameService @GameService} 注解的微服务。
 * </p>
 * <p>
 * 原理: 同一 JVM 内为每个服务创建独立的 Spring ApplicationContext,
 * 每个服务拥有自己的端口、Dubbo 注册、数据源等配置。
 * </p>
 * <p>
 * 使用方式:
 * <ul>
 *     <li>IDEA 直接运行 main() → 启动所有 enabled=true 的服务</li>
 *     <li>Program Arguments: service-game service-login → 只启动指定服务</li>
 *     <li>Program Arguments: --exclude=service-robot → 排除指定服务</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
public class LauncherApplication {

    /** 所有已启动的 Spring Context (服务名 → Context) */
    private static final Map<String, ConfigurableApplicationContext> CONTEXTS = new ConcurrentHashMap<>();

    /** 已发现的服务信息 (按 order 排序) */
    private static final List<ServiceInfo> DISCOVERED_SERVICES = new ArrayList<>();

    public static void main(String[] args) {
        printBanner();

        // 1. 解析命令行参数
        Set<String> onlyServices = new LinkedHashSet<>();
        Set<String> excludeServices = new LinkedHashSet<>();
        parseArgs(args, onlyServices, excludeServices);

        // 2. 扫描 classpath 发现所有 @GameService 标注的类
        System.out.println("[1/4] 扫描 @GameService 注解...");
        List<ServiceInfo> allServices = scanGameServices();
        DISCOVERED_SERVICES.addAll(allServices);

        if (allServices.isEmpty()) {
            System.out.println("  未发现任何 @GameService 标记的启动类！请检查依赖配置。");
            return;
        }
        System.out.println("  发现 " + allServices.size() + " 个服务:");
        for (ServiceInfo svc : allServices) {
            System.out.printf("    [%3d] %-22s %s%s%n",
                    svc.order, svc.name, svc.description,
                    svc.enabled ? "" : " (默认禁用)");
        }
        System.out.println();

        // 3. 过滤出本次要启动的服务
        List<ServiceInfo> toStart = filterServices(allServices, onlyServices, excludeServices);
        if (toStart.isEmpty()) {
            System.out.println("  没有需要启动的服务。");
            printUsage();
            return;
        }

        // 4. 启动基础设施 (Nacos)
        System.out.println("[2/4] 启动基础设施...");
        try {
            EmbeddedNacos.ensureRunning();
        } catch (Exception e) {
            System.err.println("  [!!] Nacos 启动失败: " + e.getMessage());
            System.err.println("  提示: 可手动启动 Nacos 后重试，或检查 JAVA_HOME 环境变量");
        }
        System.out.println();

        // 5. 检查基础设施
        System.out.println("[3/4] 检查基础设施...");
        checkInfrastructure();
        System.out.println();

        // 6. 按 order 顺序逐个启动
        System.out.println("[4/4] 启动 " + toStart.size() + " 个微服务...");
        System.out.println("================================================================");

        // 注册 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("正在关闭所有服务...");
            shutdownAll();
            // 最后关闭 Nacos
            EmbeddedNacos.stop();
            System.out.println("所有服务已关闭。");
        }));

        boolean allOk = startServices(toStart);

        // 6. 打印启动结果
        System.out.println();
        System.out.println("================================================================");
        if (allOk) {
            System.out.println("  所有服务启动成功！共 " + CONTEXTS.size() + " 个服务运行中");
        } else {
            System.out.println("  启动完成，部分服务可能失败，请检查日志");
        }
        System.out.println("================================================================");
        System.out.println();
        printStatus();

        // 7. 进入交互模式
        System.out.println("命令: status | stop <name> | start <name> | restart <name> | list | nacos <cmd> | exit");
        System.out.println();
        interactiveLoop();
    }

    // ======================== 扫描 ========================

    /**
     * 已知的服务启动类全限定名 (兜底列表)
     * <p>
     * 当 classpath 扫描失败时，直接尝试加载这些类。
     * 新增服务只需在此添加一行即可。
     * </p>
     */
    private static final String[] KNOWN_SERVICE_CLASSES = {
            "com.game.gateway.GatewayApplication",
            "com.game.service.login.LoginServiceApplication",
            "com.game.service.game.GameServiceApplication",
            "com.game.service.guild.GuildServiceApplication",
            "com.game.service.chat.ChatServiceApplication",
            "com.game.service.rank.RankServiceApplication",
            "com.game.service.scheduler.SchedulerApplication",
            "com.game.service.activity.ActivityServiceApplication",
            "com.game.service.pay.PayServiceApplication",
            "com.game.service.battle.BattleServiceApplication",
            "com.game.service.gm.GmServiceApplication",
            "com.game.robot.RobotApplication",
    };

    /**
     * 扫描 classpath 中所有带 @GameService 注解的类
     * <p>
     * 策略:
     * 1. 先用 Spring ClassPathScanning (与 @ComponentScan 内部机制相同)
     * 2. 若未发现，回退到直接按类名加载已知服务列表
     * </p>
     */
    private static List<ServiceInfo> scanGameServices() {
        List<ServiceInfo> result = new ArrayList<>();

        // ---- 策略一: Spring ClassPath 扫描 ----
        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(GameService.class));

            // 扫描多个根包 (覆盖 com.game.gateway, com.game.service.*, com.game.robot)
            for (String basePackage : List.of("com.game.gateway", "com.game.service", "com.game.robot")) {
                for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
                    addFromClassName(result, bd.getBeanClassName());
                }
            }
        } catch (Exception e) {
            System.err.println("  ClassPath 扫描异常: " + e.getMessage());
        }

        // ---- 策略二: 兜底直接类加载 ----
        if (result.isEmpty()) {
            System.out.println("  ClassPath 扫描未命中，使用直接类加载...");
            for (String className : KNOWN_SERVICE_CLASSES) {
                addFromClassName(result, className);
            }
        }

        // 按 order 排序
        result.sort(Comparator.comparingInt(s -> s.order));
        return result;
    }

    /**
     * 尝试加载指定类名，读取 @GameService 注解信息并加入列表
     */
    private static void addFromClassName(List<ServiceInfo> result, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            GameService anno = clazz.getAnnotation(GameService.class);
            if (anno != null) {
                // 避免重复
                boolean exists = result.stream().anyMatch(s -> s.name.equals(anno.name()));
                if (!exists) {
                    result.add(new ServiceInfo(anno.name(), anno.order(), anno.description(), anno.enabled(), clazz));
                }
            }
        } catch (ClassNotFoundException e) {
            // 该模块未在 classpath 中，跳过
        } catch (NoClassDefFoundError e) {
            // 类存在但缺少依赖，跳过
        }
    }

    // ======================== 启动 ========================

    /**
     * 按顺序启动服务列表
     */
    private static boolean startServices(List<ServiceInfo> services) {
        boolean allOk = true;
        for (ServiceInfo svc : services) {
            if (!startService(svc)) {
                allOk = false;
            }
        }
        return allOk;
    }

    /**
     * 启动单个服务
     */
    private static boolean startService(ServiceInfo svc) {
        if (CONTEXTS.containsKey(svc.name)) {
            System.out.printf("  %-22s 已在运行中，跳过%n", svc.name);
            return true;
        }

        System.out.printf("  %-22s 启动中...%n", svc.name);
        long start = System.currentTimeMillis();

        try {
            // 使用 SpringApplicationBuilder 创建独立 Context
            // 关键: setRegisterShutdownHook(false) 让我们自己管理生命周期
            SpringApplication app = new SpringApplicationBuilder(svc.clazz)
                    .registerShutdownHook(false)
                    .logStartupInfo(true)
                    .build();

            // 关闭 Banner 避免刷屏
            app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);

            ConfigurableApplicationContext ctx = app.run();
            CONTEXTS.put(svc.name, ctx);

            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("  %-22s 启动成功 (%dms)%n", svc.name, elapsed);
            return true;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("  %-22s 启动失败 (%dms): %s%n", svc.name, elapsed, e.getMessage());
            return false;
        }
    }

    // ======================== 停止 ========================

    /**
     * 停止单个服务
     */
    private static void stopService(String name) {
        ConfigurableApplicationContext ctx = CONTEXTS.remove(name);
        if (ctx != null && ctx.isActive()) {
            System.out.printf("  %-22s 停止中...%n", name);
            try {
                ctx.close();
                System.out.printf("  %-22s 已停止%n", name);
            } catch (Exception e) {
                System.out.printf("  %-22s 停止异常: %s%n", name, e.getMessage());
            }
        } else {
            System.out.printf("  %-22s 未在运行%n", name);
        }
    }

    /**
     * 停止所有服务 (逆序)
     */
    private static void shutdownAll() {
        List<String> names = new ArrayList<>(CONTEXTS.keySet());
        // 逆序关闭 (后启动的先关)
        Collections.reverse(names);
        for (String name : names) {
            ConfigurableApplicationContext ctx = CONTEXTS.remove(name);
            if (ctx != null && ctx.isActive()) {
                try {
                    ctx.close();
                    System.out.println("  已关闭: " + name);
                } catch (Exception e) {
                    System.out.println("  关闭异常: " + name + " - " + e.getMessage());
                }
            }
        }
    }

    // ======================== 基础设施检查 ========================

    private static void checkInfrastructure() {
        checkPort("Nacos", 8848);
        checkPort("MongoDB", 27017);
        checkPort("Redis", 6379);
    }

    private static void checkPort(String name, int port) {
        boolean ok = isPortOpen(port);
        System.out.printf("  [%s] %-14s localhost:%-5d %s%n",
                ok ? "OK" : "!!", name, port,
                ok ? "就绪" : "未运行 (相关服务可能受影响)");
    }

    private static boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== 交互模式 ========================

    private static void interactiveLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("launcher> ");
                String line = reader.readLine();
                if (line == null) {
                    // IDEA 中 EOF 不退出，保持运行
                    new CountDownLatch(1).await();
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

                switch (cmd) {
                    case "exit", "quit" -> {
                        System.out.println("正在关闭所有服务...");
                        shutdownAll();
                        EmbeddedNacos.stop();
                        System.out.println("再见！");
                        System.exit(0);
                    }
                    case "status", "s" -> printStatus();
                    case "list", "ls" -> printList();
                    case "stop" -> {
                        if (parts.length < 2) {
                            System.out.println("用法: stop <服务名|all>");
                        } else if ("all".equals(parts[1])) {
                            shutdownAll();
                        } else {
                            stopService(parts[1]);
                        }
                    }
                    case "start" -> {
                        if (parts.length < 2) {
                            System.out.println("用法: start <服务名|all>");
                        } else if ("all".equals(parts[1])) {
                            List<ServiceInfo> stopped = DISCOVERED_SERVICES.stream()
                                    .filter(si -> si.enabled && !CONTEXTS.containsKey(si.name))
                                    .toList();
                            startServices(stopped);
                        } else {
                            DISCOVERED_SERVICES.stream()
                                    .filter(si -> si.name.equals(parts[1]))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            LauncherApplication::startService,
                                            () -> System.out.println("未知服务: " + parts[1])
                                    );
                        }
                    }
                    case "restart" -> {
                        if (parts.length < 2) {
                            System.out.println("用法: restart <服务名>");
                        } else {
                            String svcName = parts[1];
                            stopService(svcName);
                            DISCOVERED_SERVICES.stream()
                                    .filter(si -> si.name.equals(svcName))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            LauncherApplication::startService,
                                            () -> System.out.println("未知服务: " + svcName)
                                    );
                        }
                    }
                    case "nacos" -> {
                        String subCmd = parts.length >= 2 ? parts[1].toLowerCase() : "status";
                        switch (subCmd) {
                            case "start" -> {
                                try {
                                    EmbeddedNacos.ensureRunning();
                                } catch (Exception e) {
                                    System.err.println("Nacos 启动失败: " + e.getMessage());
                                }
                            }
                            case "stop" -> EmbeddedNacos.stop();
                            case "restart" -> {
                                EmbeddedNacos.stop();
                                try {
                                    Thread.sleep(2000);
                                    EmbeddedNacos.ensureRunning();
                                } catch (Exception e) {
                                    System.err.println("Nacos 重启失败: " + e.getMessage());
                                }
                            }
                            case "status" -> {
                                boolean running = isPortOpen(8848);
                                boolean managed = EmbeddedNacos.isManagedByUs();
                                System.out.printf("  Nacos: %s%s%n",
                                        running ? "运行中" : "未运行",
                                        managed ? " (由 Launcher 管理)" : running ? " (外部实例)" : "");
                            }
                            default -> System.out.println("用法: nacos [start|stop|restart|status]");
                        }
                    }
                    case "help", "h" -> printHelp();
                    default -> System.out.println("未知命令: " + cmd + " (输入 help 查看帮助)");
                }

            } catch (Exception e) {
                if (e instanceof InterruptedException) break;
                System.err.println("错误: " + e.getMessage());
            }
        }
    }

    // ======================== 参数解析 ========================

    private static void parseArgs(String[] args, Set<String> only, Set<String> exclude) {
        for (String arg : args) {
            if (arg.startsWith("--exclude=")) {
                String[] names = arg.substring("--exclude=".length()).split(",");
                exclude.addAll(Arrays.asList(names));
            } else if (!arg.startsWith("-")) {
                only.add(arg);
            }
        }
    }

    private static List<ServiceInfo> filterServices(List<ServiceInfo> all,
                                                     Set<String> only,
                                                     Set<String> exclude) {
        return all.stream()
                .filter(svc -> {
                    // 如果指定了 only，只启动指定的
                    if (!only.isEmpty()) return only.contains(svc.name);
                    // 否则排除 exclude 和 enabled=false
                    return svc.enabled && !exclude.contains(svc.name);
                })
                .toList();
    }

    // ======================== 打印输出 ========================

    private static void printBanner() {
        System.out.println();
        System.out.println("================================================================");
        System.out.println("   ___    _    __  __  ___   ___  ___  ___ __   __ ___  ___  ");
        System.out.println("  / __|  /_\\  |  \\/  || __| / __|| __|| _ \\\\ \\ / /| __|| _ \\ ");
        System.out.println(" | (_ | / _ \\ | |\\/| || _|  \\__ \\| _| |   / \\ V / | _| |   / ");
        System.out.println("  \\___|/_/ \\_\\|_|  |_||___| |___/|___||_|_\\  \\_/  |___||_|_\\ ");
        System.out.println();
        System.out.println("       一键启动器 v3.0 | @GameService 自动发现 | Nacos 自管理");
        System.out.println("================================================================");
        System.out.println();
    }

    private static void printStatus() {
        System.out.println();
        System.out.println("基础设施:");
        System.out.println("────────────────────────────────────────────");
        boolean nacosRunning = isPortOpen(8848);
        System.out.printf("  [%s] Nacos %s%n",
                nacosRunning ? "OK" : "!!",
                nacosRunning
                        ? (EmbeddedNacos.isManagedByUs() ? "(Launcher 管理)" : "(外部实例)")
                        : "(未运行)");
        System.out.println();
        System.out.println("运行中的服务:");
        System.out.println("────────────────────────────────────────────");
        if (CONTEXTS.isEmpty()) {
            System.out.println("  (无)");
        } else {
            for (var entry : CONTEXTS.entrySet()) {
                boolean active = entry.getValue().isActive();
                System.out.printf("  [%s] %s%n", active ? "OK" : "!!", entry.getKey());
            }
        }
        System.out.println("────────────────────────────────────────────");
        System.out.println("总计: " + CONTEXTS.size() + " 个服务运行中");
        System.out.println();
    }

    private static void printList() {
        System.out.println();
        System.out.println("已发现的服务:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.printf("  %-5s %-22s %-14s %s%n", "顺序", "服务名", "描述", "状态");
        System.out.println("────────────────────────────────────────────────────────────");
        for (ServiceInfo svc : DISCOVERED_SERVICES) {
            String status;
            if (CONTEXTS.containsKey(svc.name)) {
                status = "运行中";
            } else if (!svc.enabled) {
                status = "已禁用";
            } else {
                status = "未启动";
            }
            System.out.printf("  %-5d %-22s %-14s %s%n", svc.order, svc.name, svc.description, status);
        }
        System.out.println();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("可用命令:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("  status / s              查看运行中的服务");
        System.out.println("  list / ls               列出所有已发现的服务");
        System.out.println("  start <服务名|all>      启动服务");
        System.out.println("  stop <服务名|all>       停止服务");
        System.out.println("  restart <服务名>        重启服务");
        System.out.println("  nacos [start|stop|restart|status]  管理 Nacos");
        System.out.println("  exit / quit             停止所有服务并退出");
        System.out.println("  help / h                显示此帮助");
        System.out.println();
        System.out.println("启动参数 (IDEA Program Arguments):");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("  service-game service-login    只启动指定服务");
        System.out.println("  --exclude=service-robot       排除指定服务");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("提示: 使用 Program Arguments 指定启动服务:");
        System.out.println("  service-game service-login    只启动这两个");
        System.out.println("  --exclude=service-robot       排除指定服务");
        System.out.println();
    }

    // ======================== 内部数据结构 ========================

    /**
     * 服务信息 (从 @GameService 注解读取)
     */
    record ServiceInfo(String name, int order, String description, boolean enabled, Class<?> clazz) {}
}
