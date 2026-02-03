package com.game.launcher.discovery;

import com.game.launcher.config.LauncherConfig;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务自动发现
 * <p>
 * 扫描 services/ 目录，自动发现所有可用的微服务模块
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class ServiceDiscovery {

    private static final Pattern PORT_PATTERN = Pattern.compile("port:\\s*(\\d+)");
    private static final Pattern DUBBO_PORT_PATTERN = Pattern.compile("dubbo\\.protocol\\.port:\\s*(\\d+)|port:\\s*(\\d+)");
    private static final Pattern APP_NAME_PATTERN = Pattern.compile("name:\\s*([a-zA-Z0-9-]+)");

    private final Path projectRoot;

    public ServiceDiscovery(String projectRoot) {
        this.projectRoot = Paths.get(projectRoot);
    }

    /**
     * 发现所有服务
     */
    public List<LauncherConfig.ServiceConfig> discoverServices() {
        List<LauncherConfig.ServiceConfig> services = new ArrayList<>();

        // 扫描 services 目录 (所有微服务都在这里，包括网关)
        Path servicesDir = projectRoot.resolve("services");
        if (Files.isDirectory(servicesDir)) {
            services.addAll(scanDirectory(servicesDir, "services"));
        }

        // 按照启动顺序排序
        services.sort(Comparator.comparingInt(LauncherConfig.ServiceConfig::getOrder));

        log.info("发现 {} 个服务模块", services.size());
        return services;
    }

    /**
     * 扫描目录
     */
    private List<LauncherConfig.ServiceConfig> scanDirectory(Path dir, String prefix) {
        List<LauncherConfig.ServiceConfig> services = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path subDir : stream) {
                if (!Files.isDirectory(subDir)) {
                    continue;
                }

                String moduleName = subDir.getFileName().toString();

                // 跳过非服务目录
                if (moduleName.equals("service-launcher") ||
                        moduleName.equals("service-api") ||
                        moduleName.startsWith(".")) {
                    continue;
                }

                // 检查是否有 pom.xml
                Path pomPath = subDir.resolve("pom.xml");
                if (!Files.exists(pomPath)) {
                    continue;
                }

                // 检查是否有 Spring Boot 主类
                Path javaDir = subDir.resolve("src/main/java");
                if (!Files.isDirectory(javaDir)) {
                    continue;
                }

                // 解析服务配置
                LauncherConfig.ServiceConfig config = parseServiceConfig(subDir, prefix, moduleName);
                if (config != null) {
                    services.add(config);
                    log.debug("发现服务: {} (端口: {})", config.getName(), config.getPort());
                }
            }
        } catch (IOException e) {
            log.error("扫描目录失败: {}", dir, e);
        }

        return services;
    }

    /**
     * 解析服务配置
     */
    private LauncherConfig.ServiceConfig parseServiceConfig(Path moduleDir, String prefix, String moduleName) {
        LauncherConfig.ServiceConfig config = new LauncherConfig.ServiceConfig();

        // 基本信息
        config.setName(moduleName);
        config.setModule(prefix + "/" + moduleName);
        config.setJar("target/" + moduleName + "-1.0.0-SNAPSHOT.jar");
        config.setEnabled(true);
        config.setHealthCheckUrl("http://localhost:${port}/actuator/health");

        // 从 application.yml 读取配置
        Path appYaml = moduleDir.resolve("src/main/resources/application.yml");
        if (Files.exists(appYaml)) {
            parseApplicationYaml(appYaml, config);
        }

        // 设置启动顺序
        config.setOrder(determineStartOrder(moduleName));

        // 设置描述
        config.setDescription(getDescription(moduleName));

        // 检查 JAR 是否存在
        Path jarPath = moduleDir.resolve(config.getJar());
        if (!Files.exists(jarPath)) {
            log.debug("JAR 文件不存在: {} (需要先执行构建)", jarPath);
        }

        return config;
    }

    /**
     * 解析 application.yml
     */
    @SuppressWarnings("unchecked")
    private void parseApplicationYaml(Path yamlPath, LauncherConfig.ServiceConfig config) {
        try (InputStream is = Files.newInputStream(yamlPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);
            if (data == null) {
                return;
            }

            // 读取 server.port
            Map<String, Object> server = (Map<String, Object>) data.get("server");
            if (server != null) {
                Object port = server.get("port");
                if (port instanceof Integer) {
                    config.setPort((Integer) port);
                } else if (port instanceof String) {
                    // 可能是 ${SERVER_PORT:8080} 格式
                    String portStr = (String) port;
                    Matcher m = Pattern.compile("\\$\\{[^:]+:(\\d+)}").matcher(portStr);
                    if (m.find()) {
                        config.setPort(Integer.parseInt(m.group(1)));
                    } else if (portStr.matches("\\d+")) {
                        config.setPort(Integer.parseInt(portStr));
                    }
                }
            }

            // 读取 dubbo.protocol.port
            Map<String, Object> dubbo = (Map<String, Object>) data.get("dubbo");
            if (dubbo != null) {
                Map<String, Object> protocol = (Map<String, Object>) dubbo.get("protocol");
                if (protocol != null) {
                    Object dubboPort = protocol.get("port");
                    if (dubboPort instanceof Integer) {
                        config.setDubboPort((Integer) dubboPort);
                    } else if (dubboPort instanceof String) {
                        String portStr = (String) dubboPort;
                        Matcher m = Pattern.compile("\\$\\{[^:]+:(\\d+)}").matcher(portStr);
                        if (m.find()) {
                            config.setDubboPort(Integer.parseInt(m.group(1)));
                        }
                    }
                }
            }

            // 读取 spring.application.name
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            if (spring != null) {
                Map<String, Object> application = (Map<String, Object>) spring.get("application");
                if (application != null) {
                    Object name = application.get("name");
                    if (name instanceof String) {
                        // 可以用 spring.application.name 作为服务名
                    }
                }
            }

        } catch (Exception e) {
            log.warn("解析 application.yml 失败: {}", yamlPath, e);
        }
    }

    /**
     * 确定启动顺序
     */
    private int determineStartOrder(String moduleName) {
        // 网关最先启动
        if (moduleName.contains("gateway")) {
            return 10;
        }
        // 登录服务
        if (moduleName.contains("login")) {
            return 20;
        }
        // 核心游戏服务
        if (moduleName.contains("game")) {
            return 30;
        }
        // 公会服务
        if (moduleName.contains("guild")) {
            return 40;
        }
        // 聊天服务
        if (moduleName.contains("chat")) {
            return 50;
        }
        // 排行榜服务
        if (moduleName.contains("rank")) {
            return 60;
        }
        // 定时任务服务
        if (moduleName.contains("scheduler")) {
            return 70;
        }
        // 活动服务
        if (moduleName.contains("activity")) {
            return 80;
        }
        // 支付服务
        if (moduleName.contains("pay")) {
            return 90;
        }
        // 战斗服务
        if (moduleName.contains("battle")) {
            return 100;
        }
        // GM 服务最后启动
        if (moduleName.contains("gm")) {
            return 200;
        }
        // 其他服务
        return 100;
    }

    /**
     * 获取服务描述
     */
    private String getDescription(String moduleName) {
        return switch (moduleName) {
            case "service-gateway" -> "网关服务";
            case "service-login" -> "登录服务";
            case "service-game" -> "游戏服务";
            case "service-guild" -> "公会服务";
            case "service-chat" -> "聊天服务";
            case "service-rank" -> "排行榜服务";
            case "service-scheduler" -> "定时任务服务";
            case "service-activity" -> "活动服务";
            case "service-pay" -> "支付服务";
            case "service-battle" -> "战斗服务";
            case "service-robot" -> "机器人服务";
            case "service-gm" -> "GM后台服务";
            default -> moduleName;
        };
    }

    /**
     * 刷新服务列表 (用于热重载)
     */
    public List<String> getNewServices(List<LauncherConfig.ServiceConfig> existing) {
        List<LauncherConfig.ServiceConfig> discovered = discoverServices();
        Set<String> existingNames = new HashSet<>();
        for (LauncherConfig.ServiceConfig config : existing) {
            existingNames.add(config.getName());
        }

        List<String> newServices = new ArrayList<>();
        for (LauncherConfig.ServiceConfig config : discovered) {
            if (!existingNames.contains(config.getName())) {
                newServices.add(config.getName());
            }
        }
        return newServices;
    }
}
