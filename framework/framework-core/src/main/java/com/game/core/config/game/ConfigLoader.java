package com.game.core.config.game;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.core.event.DistributedEventBus;
import com.game.core.event.EventBus;
import com.game.core.event.EventListener;
import com.game.core.event.events.ConfigReloadEvent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏配置加载器
 * <p>
 * 功能：
 * <ul>
 *     <li>自动扫描并加载 @ConfigContainer 标注的容器</li>
 *     <li>支持 JSON 格式配置文件</li>
 *     <li>支持热更新</li>
 *     <li>支持文件监听自动重载</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 配置文件格式 (JSON):
 * [
 *   { "id": 1001, "name": "初级血瓶", "type": 1, "quality": 1 },
 *   { "id": 1002, "name": "中级血瓶", "type": 1, "quality": 2 }
 * ]
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigLoader {

    private final ApplicationContext applicationContext;
    private final ResourceLoader resourceLoader;
    private final DistributedEventBus distributedEventBus;

    /**
     * 配置文件目录
     */
    @Value("${game.config.path:classpath:config/}")
    private String configPath;

    /**
     * 是否启用文件监听
     */
    @Value("${game.config.watch:false}")
    private boolean watchEnabled;

    /**
     * JSON 解析器
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 所有配置容器
     */
    @Getter
    private final Map<Class<?>, BaseConfigContainer<?>> containers = new ConcurrentHashMap<>();

    /**
     * 文件 -> 容器映射
     */
    private final Map<String, ContainerInfo> fileContainerMap = new ConcurrentHashMap<>();

    /**
     * 文件监听器
     */
    private WatchService watchService;

    @PostConstruct
    public void init() {
        scanContainers();
        loadAllConfigs();

        if (watchEnabled) {
            startFileWatcher();
        }
    }

    /**
     * 扫描所有配置容器
     */
    @SuppressWarnings("rawtypes")
    private void scanContainers() {
        Map<String, BaseConfigContainer> beans = applicationContext.getBeansOfType(BaseConfigContainer.class);

        for (BaseConfigContainer<?> container : beans.values()) {
            Class<?> containerClass = container.getClass();
            ConfigContainer annotation = containerClass.getAnnotation(ConfigContainer.class);

            if (annotation != null) {
                containers.put(containerClass, container);
                fileContainerMap.put(annotation.file(),
                        new ContainerInfo(container, annotation.configClass()));

                log.debug("发现配置容器: {}, file={}", containerClass.getSimpleName(), annotation.file());
            }
        }

        log.info("扫描配置容器完成，共 {} 个", containers.size());
    }

    /**
     * 加载所有配置
     */
    public void loadAllConfigs() {
        long startTime = System.currentTimeMillis();
        int loadedCount = 0;

        for (Map.Entry<String, ContainerInfo> entry : fileContainerMap.entrySet()) {
            String file = entry.getKey();
            ContainerInfo info = entry.getValue();

            try {
                loadConfig(file, info);
                loadedCount++;
            } catch (Exception e) {
                log.error("加载配置失败: file={}", file, e);
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        log.info("加载所有配置完成: count={}, cost={}ms", loadedCount, cost);
    }

    /**
     * 加载单个配置
     */
    @SuppressWarnings("unchecked")
    private void loadConfig(String file, ContainerInfo info) throws IOException {
        String resourcePath = configPath + file;
        Resource resource = resourceLoader.getResource(resourcePath);

        if (!resource.exists()) {
            log.warn("配置文件不存在: {}", resourcePath);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            // 解析 JSON 数组
            List<?> configs = objectMapper.readValue(is,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, info.configClass));

            // 加载到容器
            ((BaseConfigContainer<GameConfig>) info.container).load((List<GameConfig>) configs);
        }
    }

    /**
     * 重新加载指定配置
     */
    @SuppressWarnings("unchecked")
    public void reload(String file) {
        ContainerInfo info = fileContainerMap.get(file);
        if (info == null) {
            log.warn("未找到配置容器: file={}", file);
            return;
        }

        try {
            loadConfig(file, info);
            log.info("热更新配置成功: file={}", file);
        } catch (Exception e) {
            log.error("热更新配置失败: file={}", file, e);
        }
    }

    /**
     * 重新加载所有配置
     */
    public void reloadAll() {
        log.info("开始热更新所有配置...");
        loadAllConfigs();
    }

    /**
     * 重新加载并广播到集群
     */
    public void reloadAndBroadcast(String file, String operator) {
        reload(file);
        // 广播到其他服务实例
        distributedEventBus.broadcast(new ConfigReloadEvent(file, operator));
        log.info("配置热更新并广播: file={}, operator={}", file, operator);
    }

    /**
     * 重新加载所有并广播到集群
     */
    public void reloadAllAndBroadcast(String operator) {
        reloadAll();
        // 广播到其他服务实例
        distributedEventBus.broadcast(ConfigReloadEvent.reloadAll(operator));
        log.info("全量配置热更新并广播: operator={}", operator);
    }

    /**
     * 监听配置重载事件 (来自其他服务实例)
     */
    @EventListener(desc = "配置热更新事件监听")
    public void onConfigReload(ConfigReloadEvent event) {
        log.info("收到配置重载事件: file={}, operator={}, version={}",
                event.getConfigFile(), event.getOperator(), event.getVersion());

        if (event.isReloadAll()) {
            reloadAll();
        } else {
            reload(event.getConfigFile());
        }
    }

    /**
     * 获取配置容器
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseConfigContainer<?>> T getContainer(Class<T> containerClass) {
        return (T) containers.get(containerClass);
    }

    /**
     * 启动文件监听器
     */
    private void startFileWatcher() {
        try {
            if (configPath.startsWith("classpath:")) {
                log.info("配置路径为 classpath，跳过文件监听");
                return;
            }

            Path path = Paths.get(configPath);
            if (!Files.exists(path)) {
                log.warn("配置目录不存在，跳过文件监听: {}", configPath);
                return;
            }

            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            Thread watchThread = new Thread(this::watchLoop, "config-watcher");
            watchThread.setDaemon(true);
            watchThread.start();

            log.info("配置文件监听已启动: {}", configPath);

        } catch (Exception e) {
            log.error("启动配置文件监听失败", e);
        }
    }

    /**
     * 文件监听循环
     */
    private void watchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    String fileName = pathEvent.context().toString();

                    if (fileContainerMap.containsKey(fileName)) {
                        log.info("检测到配置文件变更: {}", fileName);
                        // 延迟重载，避免文件写入未完成
                        Thread.sleep(500);
                        reload(fileName);
                    }
                }

                key.reset();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("配置文件监听异常", e);
            }
        }
    }

    /**
     * 容器信息
     */
    private record ContainerInfo(BaseConfigContainer<?> container, Class<? extends GameConfig> configClass) {
    }
}
