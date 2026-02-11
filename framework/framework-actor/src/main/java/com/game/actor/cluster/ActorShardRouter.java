package com.game.actor.cluster;

import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import com.game.actor.core.ActorSystemRegistry;
import com.game.api.actor.ActorRemoteService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Actor 分片路由器
 * <p>
 * 核心组件: 根据一致性哈希自动将 Actor 消息路由到正确的物理节点。
 * 对业务代码透明 —— 调用方不需要知道 Actor 在哪台机器上。
 * </p>
 *
 * <pre>
 * 使用方式:
 * {@code
 * @Autowired
 * private ActorShardRouter shardRouter;
 *
 * // 自动路由到正确的物理机 (本地或远程)
 * shardRouter.tell("PlayerActorSystem", roleId, ActorMessage.of("ADD_GOLD", data));
 *
 * // 同步请求
 * String result = shardRouter.ask("GuildActorSystem", guildId, "GET_INFO", null, 3000);
 * }
 * </pre>
 *
 * <p>
 * 路由逻辑:
 * <ol>
 *     <li>通过一致性哈希环计算 actorId → ClusterNode</li>
 *     <li>若 ClusterNode.isLocal() → 直接投递到本地 ActorSystem</li>
 *     <li>若远程 → 通过 Dubbo ActorRemoteService RPC 转发</li>
 *     <li>若集群未启用或哈希环为空 → 退化为本地模式</li>
 * </ol>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class ActorShardRouter {

    private final ConsistentHashRing hashRing;
    private final ActorClusterConfig config;

    /**
     * Dubbo 远程调用客户端 (可为 null, 无 Dubbo 时退化本地)
     */
    private volatile ActorRemoteService remoteService;

    /**
     * 本地节点标识
     */
    private volatile String localHost;
    private volatile int localPort;

    public ActorShardRouter(ActorClusterConfig config) {
        this.config = config;
        this.hashRing = new ConsistentHashRing(config.getVirtualNodes());
    }

    // ==================== 配置注入 ====================

    /**
     * 设置 Dubbo 远程服务代理 (由 Spring 注入)
     */
    public void setRemoteService(ActorRemoteService remoteService) {
        this.remoteService = remoteService;
    }

    /**
     * 设置本地节点信息
     */
    public void setLocalNode(String host, int port) {
        this.localHost = host;
        this.localPort = port;
        log.info("ActorShardRouter 本地节点: {}:{}", host, port);
    }

    // ==================== 核心路由 API ====================

    /**
     * 向 Actor 发送消息 (fire-and-forget)
     * <p>
     * 自动路由: 本地直接投递，远程通过 Dubbo RPC 转发。
     * </p>
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorId         目标 Actor ID
     * @param message         消息
     * @return 是否成功投递
     */
    public boolean tell(String actorSystemName, long actorId, ActorMessage message) {
        // 集群未启用或无节点时退化为本地
        if (!config.isEnabled() || hashRing.isEmpty()) {
            return tellLocal(actorSystemName, actorId, message);
        }

        ClusterNode targetNode = hashRing.locate(actorId);
        if (targetNode == null) {
            log.warn("哈希环定位失败，退化为本地: actorSystem={}, actorId={}", actorSystemName, actorId);
            return tellLocal(actorSystemName, actorId, message);
        }

        if (targetNode.isLocal(localHost, localPort)) {
            return tellLocal(actorSystemName, actorId, message);
        } else {
            return tellRemote(targetNode, actorSystemName, actorId, message);
        }
    }

    /**
     * 向 Actor 发送消息 (便捷方法)
     */
    public boolean tell(String actorSystemName, long actorId, String messageType, Object data) {
        return tell(actorSystemName, actorId, ActorMessage.of(messageType, data));
    }

    /**
     * 向 Actor 发送消息并等待结果 (ask 模式)
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorId         目标 Actor ID
     * @param messageType     消息类型
     * @param jsonData        消息数据 (JSON 字符串)
     * @param timeoutMs       超时毫秒
     * @return 结果字符串，或 null
     */
    public String ask(String actorSystemName, long actorId, String messageType, String jsonData, long timeoutMs) {
        if (!config.isEnabled() || hashRing.isEmpty()) {
            return askLocal(actorSystemName, actorId, messageType, jsonData, timeoutMs);
        }

        ClusterNode targetNode = hashRing.locate(actorId);
        if (targetNode == null) {
            return askLocal(actorSystemName, actorId, messageType, jsonData, timeoutMs);
        }

        if (targetNode.isLocal(localHost, localPort)) {
            return askLocal(actorSystemName, actorId, messageType, jsonData, timeoutMs);
        } else {
            return askRemote(targetNode, actorSystemName, actorId, messageType, jsonData, timeoutMs);
        }
    }

    /**
     * 查询 Actor 归属的节点
     *
     * @param actorId Actor ID
     * @return 归属节点, 或 null (集群未启用时)
     */
    public ClusterNode getOwnerNode(long actorId) {
        if (!config.isEnabled() || hashRing.isEmpty()) {
            return null;
        }
        return hashRing.locate(actorId);
    }

    /**
     * 判断指定 Actor 是否在本地节点
     */
    public boolean isLocal(long actorId) {
        if (!config.isEnabled() || hashRing.isEmpty()) {
            return true; // 非集群模式默认本地
        }
        ClusterNode node = hashRing.locate(actorId);
        return node == null || node.isLocal(localHost, localPort);
    }

    // ==================== 哈希环管理 (委托) ====================

    /**
     * 获取哈希环 (供 ActorClusterListener 操作)
     */
    public ConsistentHashRing getHashRing() {
        return hashRing;
    }

    /**
     * 获取当前集群节点列表
     */
    public Collection<ClusterNode> getClusterNodes() {
        return hashRing.getAllNodes();
    }

    /**
     * 获取集群节点数
     */
    public int getClusterSize() {
        return hashRing.getNodeCount();
    }

    // ==================== 本地投递 ====================

    private boolean tellLocal(String actorSystemName, long actorId, ActorMessage message) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            log.warn("本地 ActorSystem 不存在: name={}, actorId={}", actorSystemName, actorId);
            return false;
        }
        return actorSystem.tell(actorId, message);
    }

    private String askLocal(String actorSystemName, long actorId, String messageType, String jsonData, long timeoutMs) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            log.warn("本地 ActorSystem 不存在 (ask): name={}, actorId={}", actorSystemName, actorId);
            return null;
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        ActorMessage message = ActorMessage.of(messageType, jsonData, future);
        boolean sent = actorSystem.tell(actorId, message);
        if (!sent) {
            return null;
        }

        try {
            Object result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("本地 ask 超时或异常: actorSystem={}, actorId={}, msgType={}",
                    actorSystemName, actorId, messageType, e);
            return null;
        }
    }

    // ==================== 远程投递 ====================

    private boolean tellRemote(ClusterNode targetNode, String actorSystemName, long actorId, ActorMessage message) {
        if (remoteService == null) {
            log.error("远程 Actor 投递失败: Dubbo ActorRemoteService 未注入, target={}, actorId={}",
                    targetNode.getNodeId(), actorId);
            return false;
        }

        try {
            String jsonData = message.getData() != null ? message.getData().toString() : null;
            boolean result = remoteService.tell(actorSystemName, actorId, message.getType(), jsonData);
            if (!result) {
                log.warn("远程 Actor 投递返回 false: target={}, actorSystem={}, actorId={}",
                        targetNode.getNodeId(), actorSystemName, actorId);
            }
            return result;
        } catch (Exception e) {
            log.error("远程 Actor 投递异常: target={}, actorSystem={}, actorId={}",
                    targetNode.getNodeId(), actorSystemName, actorId, e);
            return false;
        }
    }

    private String askRemote(ClusterNode targetNode, String actorSystemName, long actorId,
                             String messageType, String jsonData, long timeoutMs) {
        if (remoteService == null) {
            log.error("远程 Actor ask 失败: Dubbo ActorRemoteService 未注入, target={}", targetNode.getNodeId());
            return null;
        }

        try {
            return remoteService.ask(actorSystemName, actorId, messageType, jsonData, timeoutMs);
        } catch (Exception e) {
            log.error("远程 Actor ask 异常: target={}, actorSystem={}, actorId={}",
                    targetNode.getNodeId(), actorSystemName, actorId, e);
            return null;
        }
    }
}
