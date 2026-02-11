package com.game.actor.cluster;

import com.game.actor.core.ActorSystemRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Actor 集群拓扑监听器
 * <p>
 * 监听集群节点变更 (通过外部推送或定时刷新)，动态维护一致性哈希环。
 * </p>
 * <p>
 * 工作模式:
 * <ul>
 *     <li>被动: 外部调用 {@link #onInstancesChanged(List)} 推送实例变更</li>
 *     <li>主动: 定时调用注册的 {@link InstanceFetcher} 刷新拓扑</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class ActorClusterListener {

    private final ActorShardRouter shardRouter;
    private final ActorClusterConfig config;
    private final ScheduledExecutorService scheduler;

    /**
     * 实例拉取器 (可选, 用于定时刷新)
     */
    private volatile InstanceFetcher instanceFetcher;

    /**
     * 上次已知的节点 ID 集合 (用于变更检测)
     */
    private volatile Set<String> lastKnownNodeIds = Collections.emptySet();

    public ActorClusterListener(ActorShardRouter shardRouter, ActorClusterConfig config) {
        this.shardRouter = shardRouter;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "actor-cluster-listener");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动监听器
     */
    public void start() {
        if (!config.isEnabled()) {
            log.info("Actor 集群分片未启用, 跳过监听器启动");
            return;
        }

        // 定时刷新 (兜底)
        int interval = config.getRefreshIntervalSeconds();
        scheduler.scheduleAtFixedRate(this::refreshTopology, interval, interval, TimeUnit.SECONDS);

        log.info("ActorClusterListener 启动: refreshInterval={}s, virtualNodes={}",
                interval, config.getVirtualNodes());
    }

    /**
     * 停止监听器
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("ActorClusterListener 已停止");
    }

    /**
     * 设置实例拉取器 (用于定时主动刷新)
     *
     * @param fetcher 实例拉取回调
     */
    public void setInstanceFetcher(InstanceFetcher fetcher) {
        this.instanceFetcher = fetcher;
    }

    // ==================== 拓扑变更处理 ====================

    /**
     * 实例变更通知 (被动推送模式)
     * <p>
     * 当 Nacos/Dubbo 检测到服务实例变化时调用此方法。
     * </p>
     *
     * @param instances 最新的服务实例列表
     */
    public void onInstancesChanged(List<InstanceInfo> instances) {
        if (!config.isEnabled()) {
            return;
        }

        List<ClusterNode> newNodes = instances.stream()
                .map(this::toClusterNode)
                .collect(Collectors.toList());

        updateTopology(newNodes);
    }

    /**
     * 定时刷新拓扑 (主动拉取模式)
     */
    private void refreshTopology() {
        if (instanceFetcher == null) {
            return;
        }

        try {
            List<InstanceInfo> instances = instanceFetcher.fetchInstances();
            if (instances != null) {
                onInstancesChanged(instances);
            }
        } catch (Exception e) {
            log.error("定时刷新集群拓扑失败", e);
        }
    }

    /**
     * 更新拓扑: 对比变更，必要时重建哈希环
     */
    private void updateTopology(List<ClusterNode> newNodes) {
        Set<String> newNodeIds = newNodes.stream()
                .map(ClusterNode::getNodeId)
                .collect(Collectors.toSet());

        // 检查是否有变更
        if (newNodeIds.equals(lastKnownNodeIds)) {
            return; // 无变化
        }

        // 计算差异
        Set<String> added = new HashSet<>(newNodeIds);
        added.removeAll(lastKnownNodeIds);
        Set<String> removed = new HashSet<>(lastKnownNodeIds);
        removed.removeAll(newNodeIds);

        log.info("集群拓扑变更: total={}, added={}, removed={}",
                newNodes.size(), added, removed);

        // 重建哈希环
        shardRouter.getHashRing().rebuild(newNodes);
        lastKnownNodeIds = newNodeIds;

        // 记录当前拓扑
        for (ClusterNode node : newNodes) {
            log.info("  节点: {} systems={}", node.getNodeId(), node.getActorSystemNames());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将实例信息转换为集群节点
     */
    private ClusterNode toClusterNode(InstanceInfo instance) {
        Set<String> actorSystemNames = new HashSet<>();
        // 从元数据中读取 ActorSystem 名称列表
        String systems = instance.getMetadata().getOrDefault("actorSystems", "");
        if (!systems.isEmpty()) {
            actorSystemNames.addAll(Arrays.asList(systems.split(",")));
        }
        return new ClusterNode(instance.getHost(), instance.getPort(), actorSystemNames);
    }

    // ==================== 内部类型 ====================

    /**
     * 服务实例信息 (通用抽象, 不依赖特定注册中心 API)
     */
    public static class InstanceInfo {
        private final String host;
        private final int port;
        private final Map<String, String> metadata;

        public InstanceInfo(String host, int port) {
            this(host, port, new HashMap<>());
        }

        public InstanceInfo(String host, int port, Map<String, String> metadata) {
            this.host = host;
            this.port = port;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    /**
     * 实例拉取器接口 (函数式)
     * <p>
     * 由外部实现，用于定时获取服务实例列表。
     * 例如通过 Nacos NamingService 或 Spring DiscoveryClient。
     * </p>
     */
    @FunctionalInterface
    public interface InstanceFetcher {
        List<InstanceInfo> fetchInstances() throws Exception;
    }
}
