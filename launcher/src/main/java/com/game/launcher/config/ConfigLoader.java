package com.game.launcher.config;

import com.game.launcher.discovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 配置加载器
 *
 * @author GameServer
 */
@Slf4j
public class ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "launcher.yaml";

    /**
     * 加载配置
     */
    public static LauncherConfig load(String configPath) {
        Path path = configPath != null ? Paths.get(configPath) : findConfigFile();

        if (path != null && Files.exists(path)) {
            log.info("加载配置文件: {}", path.toAbsolutePath());
            try (InputStream is = new FileInputStream(path.toFile())) {
                LoaderOptions options = new LoaderOptions();
                Yaml yaml = new Yaml(new Constructor(LauncherConfig.class, options));
                LauncherConfig config = yaml.load(is);
                processVariables(config);
                return config;
            } catch (IOException e) {
                log.error("加载配置文件失败: {}", path, e);
            }
        }

        // 使用自动发现
        log.info("使用自动发现模式...");
        return createAutoDiscoveryConfig();
    }

    /**
     * 查找配置文件
     */
    private static Path findConfigFile() {
        // 1. 当前目录
        Path currentDir = Paths.get(DEFAULT_CONFIG_FILE);
        if (Files.exists(currentDir)) {
            return currentDir;
        }

        // 2. config 目录
        Path configDir = Paths.get("config", DEFAULT_CONFIG_FILE);
        if (Files.exists(configDir)) {
            return configDir;
        }

        // 3. classpath
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                Path tempFile = Files.createTempFile("launcher", ".yaml");
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            }
        } catch (IOException e) {
            log.debug("从 classpath 加载配置失败", e);
        }

        return null;
    }

    /**
     * 创建自动发现配置
     */
    private static LauncherConfig createAutoDiscoveryConfig() {
        LauncherConfig config = new LauncherConfig();

        // 确定项目根目录
        String projectRoot = findProjectRoot();
        config.getGlobal().setProjectRoot(projectRoot);
        config.getGlobal().setJvmArgs("-Xms256m -Xmx512m -Dfile.encoding=UTF-8");

        // 自动发现服务
        ServiceDiscovery discovery = new ServiceDiscovery(projectRoot);
        List<LauncherConfig.ServiceConfig> services = discovery.discoverServices();
        config.setServices(services);

        // 创建默认服务组
        createDefaultGroups(config);

        return config;
    }

    /**
     * 查找项目根目录
     */
    private static String findProjectRoot() {
        // 尝试从当前目录向上查找 pom.xml
        Path current = Paths.get("").toAbsolutePath();

        for (int i = 0; i < 5; i++) {
            Path pomPath = current.resolve("pom.xml");
            Path servicesPath = current.resolve("services");

            if (Files.exists(pomPath) && Files.isDirectory(servicesPath)) {
                return current.toString();
            }

            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }

        // 默认使用当前目录
        return Paths.get("").toAbsolutePath().toString();
    }

    /**
     * 创建默认服务组
     */
    private static void createDefaultGroups(LauncherConfig config) {
        // 核心服务组
        LauncherConfig.ServiceGroup core = new LauncherConfig.ServiceGroup();
        core.setName("core");
        core.setDescription("核心服务 (网关、登录、游戏)");
        core.setOrder(1);
        core.setServices(List.of("service-gateway", "service-login", "service-game"));
        config.getGroups().put("core", core);

        // 社交服务组
        LauncherConfig.ServiceGroup social = new LauncherConfig.ServiceGroup();
        social.setName("social");
        social.setDescription("社交服务 (公会、聊天)");
        social.setOrder(2);
        social.setServices(List.of("service-guild", "service-chat"));
        config.getGroups().put("social", social);

        // 辅助服务组
        LauncherConfig.ServiceGroup auxiliary = new LauncherConfig.ServiceGroup();
        auxiliary.setName("auxiliary");
        auxiliary.setDescription("辅助服务 (排行榜、定时任务)");
        auxiliary.setOrder(3);
        auxiliary.setServices(List.of("service-rank", "service-scheduler"));
        config.getGroups().put("auxiliary", auxiliary);

        // 运营服务组
        LauncherConfig.ServiceGroup operation = new LauncherConfig.ServiceGroup();
        operation.setName("operation");
        operation.setDescription("运营服务 (活动、支付、GM)");
        operation.setOrder(4);
        operation.setServices(List.of("service-activity", "service-pay", "service-gm"));
        config.getGroups().put("operation", operation);

        // 全部服务
        LauncherConfig.ServiceGroup all = new LauncherConfig.ServiceGroup();
        all.setName("all");
        all.setDescription("全部服务");
        all.setOrder(0);
        all.setServices(config.getServices().stream()
                .map(LauncherConfig.ServiceConfig::getName)
                .toList());
        config.getGroups().put("all", all);
    }

    /**
     * 处理变量替换
     */
    private static void processVariables(LauncherConfig config) {
        for (LauncherConfig.ServiceConfig service : config.getServices()) {
            if (service.getJar() != null) {
                service.setJar(service.getJar().replace("${name}", service.getName()));
            }
            if (service.getHealthCheckUrl() != null) {
                int port = service.getPort();
                service.setHealthCheckUrl(service.getHealthCheckUrl().replace("${port}", String.valueOf(port)));
            }
        }
    }

    /**
     * 重新加载配置 (支持热重载)
     */
    public static LauncherConfig reload(String configPath, LauncherConfig existingConfig) {
        LauncherConfig newConfig = load(configPath);

        // 合并新发现的服务
        if (existingConfig != null) {
            ServiceDiscovery discovery = new ServiceDiscovery(newConfig.getGlobal().getProjectRoot());
            List<String> newServices = discovery.getNewServices(existingConfig.getServices());
            if (!newServices.isEmpty()) {
                log.info("发现 {} 个新服务: {}", newServices.size(), newServices);
            }
        }

        return newConfig;
    }
}
