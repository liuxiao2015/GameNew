package com.game.core.cluster;

import com.game.common.util.JsonUtil;
import com.game.data.redis.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务注册中心
 * <p>
 * 基于 Redis 的轻量级服务注册，用于：
 * <ul>
 *     <li>记录所有服务实例信息</li>
 *     <li>服务发现和健康检查</li>
 *     <li>运维监控</li>
 * </ul>
 * </p>
 *
 * <b>注意：</b> 这不是用来替代 Nacos 的服务发现，而是用于框架内部的实例追踪。
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRegistry {

    private final RedisService redisService;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${game.server.id:1}")
    private int serverId;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${game.version:1.0.0}")
    private String version;

    /**
     * Redis Key 前缀
     */
    private static final String REGISTRY_KEY_PREFIX = "cluster:instance:";

    /**
     * 实例列表 Key
     */
    private static final String INSTANCE_SET_KEY = "cluster:instances";

    /**
     * 心跳间隔 (秒)
     */
    private static final int HEARTBEAT_INTERVAL = 10;

    /**
     * 实例过期时间 (秒)
     */
    private static final int INSTANCE_EXPIRE_SECONDS = 30;

    /**
     * 当前实例信息
     */
    private ServiceInstance currentInstance;

    /**
     * 心跳调度器
     */
    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    public void init() {
        // 构建实例信息
        currentInstance = buildCurrentInstance();

        // 注册实例
        register();

        // 启动心跳
        startHeartbeat();

        log.info("ServiceRegistry 初始化完成: instanceId={}, serviceName={}",
                currentInstance.getInstanceId(), currentInstance.getServiceName());
    }

    @PreDestroy
    public void destroy() {
        // 停止心跳
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }

        // 注销实例
        unregister();

        log.info("ServiceRegistry 已关闭: instanceId={}", currentInstance.getInstanceId());
    }

    /**
     * 构建当前实例信息
     */
    private ServiceInstance buildCurrentInstance() {
        String host = "unknown";
        String ip = "unknown";

        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
            ip = addr.getHostAddress();
        } catch (Exception e) {
            log.warn("获取主机信息失败", e);
        }

        String instanceId = applicationName + "-" + serverId + "-" + ip + "-" + serverPort;

        return ServiceInstance.builder()
                .instanceId(instanceId)
                .serviceName(applicationName)
                .serverId(serverId)
                .host(host)
                .ip(ip)
                .port(serverPort)
                .startTime(System.currentTimeMillis())
                .lastHeartbeat(System.currentTimeMillis())
                .status("UP")
                .version(version)
                .build();
    }

    /**
     * 注册实例
     */
    private void register() {
        String key = REGISTRY_KEY_PREFIX + currentInstance.getInstanceId();
        String json = JsonUtil.toJson(currentInstance);

        redisService.set(key, json, Duration.ofSeconds(INSTANCE_EXPIRE_SECONDS));
        redisService.sAdd(INSTANCE_SET_KEY, currentInstance.getInstanceId());

        log.debug("注册服务实例: {}", currentInstance.getInstanceId());
    }

    /**
     * 注销实例
     */
    private void unregister() {
        String key = REGISTRY_KEY_PREFIX + currentInstance.getInstanceId();
        redisService.delete(key);
        redisService.sRemove(INSTANCE_SET_KEY, currentInstance.getInstanceId());

        log.debug("注销服务实例: {}", currentInstance.getInstanceId());
    }

    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "service-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatScheduler.scheduleAtFixedRate(this::heartbeat,
                HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 发送心跳
     */
    private void heartbeat() {
        try {
            currentInstance.setLastHeartbeat(System.currentTimeMillis());
            register();
        } catch (Exception e) {
            log.error("心跳发送失败", e);
        }
    }

    /**
     * 获取当前实例信息
     */
    public ServiceInstance getCurrentInstance() {
        return currentInstance;
    }

    /**
     * 获取所有实例
     */
    public List<ServiceInstance> getAllInstances() {
        Set<String> instanceIds = redisService.sMembers(INSTANCE_SET_KEY);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ServiceInstance> instances = new ArrayList<>();
        for (String instanceId : instanceIds) {
            String key = REGISTRY_KEY_PREFIX + instanceId;
            String json = redisService.get(key);

            if (json != null) {
                ServiceInstance instance = JsonUtil.fromJson(json, ServiceInstance.class);
                if (instance != null) {
                    instances.add(instance);
                }
            } else {
                // 清理过期的实例 ID
                redisService.sRemove(INSTANCE_SET_KEY, instanceId);
            }
        }

        return instances;
    }

    /**
     * 获取指定服务的所有实例
     */
    public List<ServiceInstance> getInstancesByService(String serviceName) {
        return getAllInstances().stream()
                .filter(i -> serviceName.equals(i.getServiceName()))
                .toList();
    }

    /**
     * 获取在线实例数
     */
    public int getOnlineInstanceCount() {
        return (int) getAllInstances().stream()
                .filter(ServiceInstance::isOnline)
                .count();
    }

    /**
     * 获取指定服务的在线实例数
     */
    public int getOnlineInstanceCount(String serviceName) {
        return (int) getInstancesByService(serviceName).stream()
                .filter(ServiceInstance::isOnline)
                .count();
    }
}
