package com.game.core.cluster;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Nacos 的服务注册中心
 * <p>
 * 直接使用 Spring Cloud DiscoveryClient / Dubbo Registry，无需额外 Redis 存储：
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
@RequiredArgsConstructor
public class NacosServiceRegistry {

    /**
     * Spring Cloud 服务发现客户端
     */
    private final DiscoveryClient discoveryClient;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${game.server.id:1}")
    private int serverId;

    @PostConstruct
    public void init() {
        log.info("NacosServiceRegistry 初始化完成: applicationName={}, serverId={}", 
                applicationName, serverId);
    }

    // ==================== 服务发现 ====================

    /**
     * 获取所有服务名称
     */
    public List<String> getServices() {
        return discoveryClient.getServices();
    }

    /**
     * 获取指定服务的所有实例
     */
    public List<ServiceInstance> getInstances(String serviceName) {
        return discoveryClient.getInstances(serviceName);
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

    // ==================== 统计信息 ====================

    /**
     * 获取所有服务实例统计
     */
    public Map<String, Integer> getServiceInstanceCounts() {
        return getServices().stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> getInstances(name).size()
                ));
    }

    /**
     * 获取游戏服务实例信息
     */
    public List<GameServiceInstance> getGameServiceInstances(String serviceName) {
        return getInstances(serviceName).stream()
                .map(this::toGameServiceInstance)
                .toList();
    }

    /**
     * 转换为游戏服务实例
     */
    private GameServiceInstance toGameServiceInstance(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        
        return GameServiceInstance.builder()
                .instanceId(instance.getInstanceId())
                .serviceName(instance.getServiceId())
                .host(instance.getHost())
                .port(instance.getPort())
                .serverId(parseServerId(metadata.get("serverId")))
                .version(metadata.getOrDefault("version", "unknown"))
                .startTime(parseLong(metadata.get("startTime"), System.currentTimeMillis()))
                .build();
    }

    private int parseServerId(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(String value, long defaultValue) {
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ==================== 游戏服务实例 ====================

    /**
     * 游戏服务实例信息
     */
    @lombok.Data
    @lombok.Builder
    public static class GameServiceInstance {
        private String instanceId;
        private String serviceName;
        private String host;
        private int port;
        private int serverId;
        private String version;
        private long startTime;
    }
}
