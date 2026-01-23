package com.game.core.config.game;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.core.event.DistributedEventBus;
import com.game.core.event.EventBus;
import com.game.core.event.EventListener;
import com.game.core.event.events.ConfigReloadEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏配置加载器 (增强版)
 * <p>
 * 功能：
 * <ul>
 *     <li>自动扫描并加载 @ConfigContainer 标注的容器</li>
 *     <li>支持 JSON 格式配置文件</li>
 *     <li>支持热更新 (本地/远程)</li>
 *     <li>支持文件监听自动重载</li>
 *     <li>支持版本控制和回滚</li>
 *     <li>支持 MD5 校验</li>
 *     <li>支持预验证</li>
 *     <li>支持远程加载 (HTTP)</li>
 *     <li>支持集群广播</li>
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
     * 远程配置 URL (可选)
     */
    @Value("${game.config.remote-url:}")
    private String remoteConfigUrl;

    /**
     * 是否启用文件监听
     */
    @Value("${game.config.watch:false}")
    private boolean watchEnabled;

    /**
     * 最大备份版本数
     */
    @Value("${game.config.max-backup:5}")
    private int maxBackupVersions;

    /**
     * JSON 解析器
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * HTTP 客户端 (用于远程加载)
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
     * 配置版本历史 (file -> list of versions)
     */
    private final Map<String, LinkedList<ConfigVersion>> versionHistory = new ConcurrentHashMap<>();

    /**
     * 当前版本号
     */
    private final Map<String, Long> currentVersions = new ConcurrentHashMap<>();

    /**
     * 文件监听器
     */
    private WatchService watchService;

    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        scanContainers();
        loadAllConfigs();

        if (watchEnabled) {
            startFileWatcher();
        }
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭文件监听器失败", e);
            }
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
    public Map<String, ConfigLoadResult> loadAllConfigs() {
        long startTime = System.currentTimeMillis();
        Map<String, ConfigLoadResult> results = new HashMap<>();

        for (Map.Entry<String, ContainerInfo> entry : fileContainerMap.entrySet()) {
            String file = entry.getKey();
            ConfigLoadResult result = loadConfigInternal(file, entry.getValue(), "startup", "local");
            results.put(file, result);
        }

        long cost = System.currentTimeMillis() - startTime;
        int successCount = (int) results.values().stream().filter(ConfigLoadResult::isSuccess).count();
        log.info("加载所有配置完成: success={}, total={}, cost={}ms", successCount, results.size(), cost);

        return results;
    }

    /**
     * 内部加载单个配置
     */
    @SuppressWarnings("unchecked")
    private ConfigLoadResult loadConfigInternal(String file, ContainerInfo info, String operator, String source) {
        long startTime = System.currentTimeMillis();

        try {
            // 读取配置内容
            String content = readConfigContent(file, source);
            if (content == null || content.isEmpty()) {
                return ConfigLoadResult.fail(file, "配置文件不存在或为空");
            }

            // 计算 MD5
            String md5 = calculateMd5(content);

            // 检查是否需要重载 (MD5 相同则跳过)
            ConfigVersion currentVersion = getCurrentVersion(file);
            if (currentVersion != null && md5.equals(currentVersion.getMd5())) {
                log.debug("配置未变更，跳过加载: file={}, md5={}", file, md5);
                return ConfigLoadResult.success(file, currentVersion.getVersion(), md5, info.container.size(), 0);
            }

            // 解析 JSON 数组
            List<?> configs;
            try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                configs = objectMapper.readValue(is,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, info.configClass));
            }

            // 预验证 (不替换原数据)
            List<String> validationErrors = new ArrayList<>();
            for (Object configObj : configs) {
                GameConfig config = (GameConfig) configObj;
                config.afterLoad();
                String error = config.validate();
                if (error != null && !error.isEmpty()) {
                    validationErrors.add("id=" + config.getId() + ": " + error);
                }
            }

            if (!validationErrors.isEmpty()) {
                log.error("配置验证失败: file={}, errors={}", file, validationErrors);
                return ConfigLoadResult.validationFail(file, validationErrors);
            }

            // 备份当前版本
            backupCurrentVersion(file, info.container);

            // 加载到容器
            ((BaseConfigContainer<GameConfig>) info.container).load((List<GameConfig>) configs);

            // 记录新版本
            long version = System.currentTimeMillis();
            currentVersions.put(file, version);
            saveVersion(file, version, md5, content, operator, source, configs.size());

            long cost = System.currentTimeMillis() - startTime;
            log.info("加载配置成功: file={}, count={}, md5={}, cost={}ms", file, configs.size(), md5, cost);

            return ConfigLoadResult.success(file, version, md5, configs.size(), cost);

        } catch (Exception e) {
            log.error("加载配置失败: file={}", file, e);
            return ConfigLoadResult.fail(file, "加载失败: " + e.getMessage());
        }
    }

    /**
     * 读取配置内容
     */
    private String readConfigContent(String file, String source) throws IOException {
        if ("remote".equals(source) && remoteConfigUrl != null && !remoteConfigUrl.isEmpty()) {
            return readFromRemote(file);
        }
        return readFromLocal(file);
    }

    /**
     * 从本地读取配置
     */
    private String readFromLocal(String file) throws IOException {
        String resourcePath = configPath + file;
        Resource resource = resourceLoader.getResource(resourcePath);

        if (!resource.exists()) {
            log.warn("配置文件不存在: {}", resourcePath);
            return null;
        }

        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 从远程读取配置
     */
    private String readFromRemote(String file) {
        String url = remoteConfigUrl + "/" + file;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("从远程加载配置成功: url={}", url);
                return response.body();
            } else {
                log.error("从远程加载配置失败: url={}, status={}", url, response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("从远程加载配置异常: url={}", url, e);
            return null;
        }
    }

    /**
     * 计算 MD5
     */
    private String calculateMd5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算 MD5 失败", e);
            return "";
        }
    }

    /**
     * 获取当前版本
     */
    public ConfigVersion getCurrentVersion(String file) {
        LinkedList<ConfigVersion> history = versionHistory.get(file);
        return (history != null && !history.isEmpty()) ? history.getFirst() : null;
    }

    /**
     * 备份当前版本
     */
    private void backupCurrentVersion(String file, BaseConfigContainer<?> container) {
        ConfigVersion current = getCurrentVersion(file);
        if (current != null && current.getContent() != null) {
            // 版本已经在历史记录中，无需额外操作
            log.debug("当前版本已备份: file={}, version={}", file, current.getVersion());
        }
    }

    /**
     * 保存版本
     */
    private void saveVersion(String file, long version, String md5, String content, 
                            String operator, String source, int configCount) {
        ConfigVersion ver = new ConfigVersion();
        ver.setConfigFile(file);
        ver.setVersion(version);
        ver.setMd5(md5);
        ver.setContent(content);
        ver.setLoadTime(System.currentTimeMillis());
        ver.setOperator(operator);
        ver.setSource(source);
        ver.setConfigCount(configCount);

        LinkedList<ConfigVersion> history = versionHistory.computeIfAbsent(file, k -> new LinkedList<>());
        history.addFirst(ver);

        // 限制版本数量
        while (history.size() > maxBackupVersions) {
            history.removeLast();
        }
    }

    /**
     * 获取版本历史
     */
    public List<ConfigVersion> getVersionHistory(String file) {
        LinkedList<ConfigVersion> history = versionHistory.get(file);
        if (history == null) {
            return Collections.emptyList();
        }
        // 返回不包含 content 的版本信息 (避免传输大量数据)
        return history.stream().map(v -> {
            ConfigVersion copy = new ConfigVersion();
            copy.setConfigFile(v.getConfigFile());
            copy.setVersion(v.getVersion());
            copy.setMd5(v.getMd5());
            copy.setLoadTime(v.getLoadTime());
            copy.setOperator(v.getOperator());
            copy.setSource(v.getSource());
            copy.setConfigCount(v.getConfigCount());
            return copy;
        }).toList();
    }

    /**
     * 回滚到指定版本
     */
    @SuppressWarnings("unchecked")
    public ConfigLoadResult rollback(String file, long targetVersion, String operator) {
        LinkedList<ConfigVersion> history = versionHistory.get(file);
        if (history == null || history.isEmpty()) {
            return ConfigLoadResult.fail(file, "无版本历史");
        }

        // 查找目标版本
        ConfigVersion targetVer = null;
        for (ConfigVersion v : history) {
            if (v.getVersion() == targetVersion) {
                targetVer = v;
                break;
            }
        }

        if (targetVer == null) {
            return ConfigLoadResult.fail(file, "目标版本不存在: " + targetVersion);
        }

        if (targetVer.getContent() == null) {
            return ConfigLoadResult.fail(file, "目标版本内容不可用");
        }

        ContainerInfo info = fileContainerMap.get(file);
        if (info == null) {
            return ConfigLoadResult.fail(file, "配置容器不存在");
        }

        long startTime = System.currentTimeMillis();

        try {
            // 解析配置
            List<?> configs;
            try (InputStream is = new ByteArrayInputStream(targetVer.getContent().getBytes(StandardCharsets.UTF_8))) {
                configs = objectMapper.readValue(is,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, info.configClass));
            }

            // 加载到容器
            ((BaseConfigContainer<GameConfig>) info.container).load((List<GameConfig>) configs);

            // 更新当前版本
            currentVersions.put(file, targetVersion);

            long cost = System.currentTimeMillis() - startTime;
            log.info("回滚配置成功: file={}, version={}, operator={}, cost={}ms", 
                    file, targetVersion, operator, cost);

            return ConfigLoadResult.success(file, targetVersion, targetVer.getMd5(), configs.size(), cost);

        } catch (Exception e) {
            log.error("回滚配置失败: file={}, version={}", file, targetVersion, e);
            return ConfigLoadResult.fail(file, "回滚失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载指定配置
     */
    public ConfigLoadResult reload(String file, String operator) {
        ContainerInfo info = fileContainerMap.get(file);
        if (info == null) {
            log.warn("未找到配置容器: file={}", file);
            return ConfigLoadResult.fail(file, "配置容器不存在");
        }

        return loadConfigInternal(file, info, operator, "local");
    }

    /**
     * 从远程重新加载指定配置
     */
    public ConfigLoadResult reloadFromRemote(String file, String operator) {
        ContainerInfo info = fileContainerMap.get(file);
        if (info == null) {
            log.warn("未找到配置容器: file={}", file);
            return ConfigLoadResult.fail(file, "配置容器不存在");
        }

        return loadConfigInternal(file, info, operator, "remote");
    }

    /**
     * 重新加载所有配置
     */
    public Map<String, ConfigLoadResult> reloadAll(String operator) {
        log.info("开始热更新所有配置: operator={}", operator);
        Map<String, ConfigLoadResult> results = new HashMap<>();
        
        for (Map.Entry<String, ContainerInfo> entry : fileContainerMap.entrySet()) {
            String file = entry.getKey();
            ConfigLoadResult result = loadConfigInternal(file, entry.getValue(), operator, "local");
            results.put(file, result);
        }
        
        return results;
    }

    /**
     * 重新加载并广播到集群
     */
    public ConfigLoadResult reloadAndBroadcast(String file, String operator) {
        ConfigLoadResult result = reload(file, operator);
        if (result.isSuccess()) {
            // 广播到其他服务实例
            distributedEventBus.broadcast(new ConfigReloadEvent(file, operator));
            log.info("配置热更新并广播: file={}, operator={}", file, operator);
        }
        return result;
    }

    /**
     * 重新加载所有并广播到集群
     */
    public Map<String, ConfigLoadResult> reloadAllAndBroadcast(String operator) {
        Map<String, ConfigLoadResult> results = reloadAll(operator);
        // 广播到其他服务实例
        distributedEventBus.broadcast(ConfigReloadEvent.reloadAll(operator));
        log.info("全量配置热更新并广播: operator={}", operator);
        return results;
    }

    /**
     * 监听配置重载事件 (来自其他服务实例)
     */
    @EventListener(desc = "配置热更新事件监听")
    public void onConfigReload(ConfigReloadEvent event) {
        log.info("收到配置重载事件: file={}, operator={}, version={}",
                event.getConfigFile(), event.getOperator(), event.getVersion());

        if (event.isReloadAll()) {
            reloadAll("cluster:" + event.getOperator());
        } else {
            reload(event.getConfigFile(), "cluster:" + event.getOperator());
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
     * 获取所有配置文件列表
     */
    public List<String> getAllConfigFiles() {
        return new ArrayList<>(fileContainerMap.keySet());
    }

    /**
     * 获取配置状态
     */
    public Map<String, Object> getConfigStatus(String file) {
        Map<String, Object> status = new HashMap<>();
        status.put("file", file);

        ContainerInfo info = fileContainerMap.get(file);
        if (info != null) {
            status.put("loaded", info.container.isLoaded());
            status.put("count", info.container.size());
            status.put("loadTime", info.container.getLoadTime());
        }

        ConfigVersion currentVersion = getCurrentVersion(file);
        if (currentVersion != null) {
            status.put("version", currentVersion.getVersion());
            status.put("md5", currentVersion.getMd5());
            status.put("operator", currentVersion.getOperator());
            status.put("source", currentVersion.getSource());
        }

        status.put("historyCount", getVersionHistory(file).size());

        return status;
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
        while (running && !Thread.currentThread().isInterrupted()) {
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
                        reload(fileName, "file-watcher");
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
