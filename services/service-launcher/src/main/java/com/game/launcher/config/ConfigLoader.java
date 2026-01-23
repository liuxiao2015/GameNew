package com.game.launcher.config;

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

        if (path == null || !Files.exists(path)) {
            log.warn("配置文件不存在，使用默认配置: {}", configPath);
            return createDefaultConfig();
        }

        log.info("加载配置文件: {}", path.toAbsolutePath());

        try (InputStream is = new FileInputStream(path.toFile())) {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(LauncherConfig.class, options));
            LauncherConfig config = yaml.load(is);

            // 处理变量替换
            processVariables(config);

            return config;
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", path, e);
            throw new RuntimeException("加载配置文件失败", e);
        }
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
                // 复制到临时文件
                Path tempFile = Files.createTempFile("launcher", ".yaml");
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            }
        } catch (IOException e) {
            log.warn("从 classpath 加载配置失败", e);
        }

        return null;
    }

    /**
     * 创建默认配置
     */
    private static LauncherConfig createDefaultConfig() {
        LauncherConfig config = new LauncherConfig();

        // 添加默认服务
        addDefaultService(config, "gateway-server", "网关服务", "gateway/gateway-server", 8888, 20880);
        addDefaultService(config, "service-login", "登录服务", "services/service-login", 8080, 20881);
        addDefaultService(config, "service-game", "游戏服务", "services/service-game", 8081, 20882);
        addDefaultService(config, "service-guild", "公会服务", "services/service-guild", 8082, 20883);
        addDefaultService(config, "service-chat", "聊天服务", "services/service-chat", 8083, 20884);
        addDefaultService(config, "service-rank", "排行榜服务", "services/service-rank", 8084, 20885);
        addDefaultService(config, "service-scheduler", "定时任务服务", "services/service-scheduler", 8085, 20886);
        addDefaultService(config, "service-gm", "GM后台服务", "services/service-gm", 8090, 0);

        return config;
    }

    /**
     * 添加默认服务配置
     */
    private static void addDefaultService(LauncherConfig config, String name, String description,
                                          String module, int port, int dubboPort) {
        LauncherConfig.ServiceConfig service = new LauncherConfig.ServiceConfig();
        service.setName(name);
        service.setDescription(description);
        service.setModule(module);
        service.setPort(port);
        service.setDubboPort(dubboPort);
        service.setJar("target/" + name + "-1.0.0-SNAPSHOT.jar");
        config.getServices().add(service);
    }

    /**
     * 处理变量替换
     */
    private static void processVariables(LauncherConfig config) {
        // 替换 ${name} 等变量
        for (LauncherConfig.ServiceConfig service : config.getServices()) {
            if (service.getJar() != null) {
                service.setJar(service.getJar().replace("${name}", service.getName()));
            }
        }
    }
}
