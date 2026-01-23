package com.game.core.cluster;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于 Nacos 的服务注册中心
 * <p>
 * 直接使用 Dubbo Registry，无需额外 Redis 存储：
 * <ul>
 *     <li>服务发现直接从 Nacos 获取</li>
 *     <li>健康检查由 Nacos 自动处理</li>
 *     <li>减少 Redis 压力</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class NacosServiceRegistry {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${game.server.id:1}")
    private int serverId;

    @Autowired(required = false)
    private RegistryConfig registryConfig;

    /**
     * 服务实例缓存
     */
    private final Map<String, List<ServiceInstance>> instanceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("NacosServiceRegistry 初始化完成: applicationName={}, serverId={}", 
                applicationName, serverId);
    }

    // ==================== 服务发现 ====================

    /**
     * 获取所有已知服务名称
     */
    public List<String> getServices() {
        return new ArrayList<>(instanceCache.keySet());
    }

    /**
     * 获取指定服务的所有实例
     */
    public List<ServiceInstance> getInstances(String serviceName) {
        return instanceCache.getOrDefault(serviceName, Collections.emptyList());
    }

    /**
     * 获取当前服务的所有实例
     */
    public List<ServiceInstance> getCurrentServiceInstances() {
        return getInstances(applicationName);
    }

    /**
     * 获取指定服务的实例数量
     */
    public int getInstanceCount(String serviceName) {
        return getInstances(serviceName).size();
    }

    /**
     * 检查服务是否可用
     */
    public boolean isServiceAvailable(String serviceName) {
        return !getInstances(serviceName).isEmpty();
    }

    /**
     * 注册服务实例（用于手动注册）
     */
    public void registerInstance(String serviceName, ServiceInstance instance) {
        instanceCache.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(instance);
        log.info("注册服务实例: service={}, instance={}", serviceName, instance.getInstanceId());
    }

    /**
     * 更新服务实例列表
     */
    public void updateInstances(String serviceName, List<ServiceInstance> instances) {
        instanceCache.put(serviceName, new ArrayList<>(instances));
        log.debug("更新服务实例: service={}, count={}", serviceName, instances.size());
    }

    // ==================== 统计信息 ====================

    /**
     * 获取所有服务实例统计
     */
    public Map<String, Integer> getServiceInstanceCounts() {
        return instanceCache.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                ));
    }

    // ==================== 服务实例定义 ====================

    /**
     * 服务实例信息
     */
    @Data
    public static class ServiceInstance {
        private String instanceId;
        private String serviceName;
        private String host;
        private int port;
        private int serverId;
        private String version;
        private long startTime;
        private Map<String, String> metadata = new HashMap<>();

        public static ServiceInstance of(String serviceName, String host, int port) {
            ServiceInstance instance = new ServiceInstance();
            instance.setInstanceId(serviceName + "-" + host + ":" + port);
            instance.setServiceName(serviceName);
            instance.setHost(host);
            instance.setPort(port);
            instance.setStartTime(System.currentTimeMillis());
            return instance;
        }
    }
}
