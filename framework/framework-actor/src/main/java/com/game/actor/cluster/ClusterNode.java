package com.game.actor.cluster;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 集群节点描述
 * <p>
 * 封装一个物理节点 (JVM 实例) 的信息，包括：
 * <ul>
 *     <li>nodeId -- 全局唯一标识 (通常为 host:dubboPort)</li>
 *     <li>host / port -- 网络地址</li>
 *     <li>actorSystemNames -- 该节点上注册的 ActorSystem 列表</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(of = "nodeId")
public class ClusterNode {

    /**
     * 节点唯一标识 (例如 "192.168.1.10:20881")
     */
    private final String nodeId;

    /**
     * 主机地址
     */
    private final String host;

    /**
     * Dubbo 端口
     */
    private final int port;

    /**
     * 该节点上托管的 ActorSystem 名称集合
     */
    private final Set<String> actorSystemNames;

    /**
     * 节点加入集群的时间戳
     */
    private final long joinTime;

    public ClusterNode(String host, int port) {
        this(host, port, new HashSet<>());
    }

    public ClusterNode(String host, int port, Set<String> actorSystemNames) {
        this.host = host;
        this.port = port;
        this.nodeId = host + ":" + port;
        this.actorSystemNames = actorSystemNames;
        this.joinTime = System.currentTimeMillis();
    }

    /**
     * 判断是否为本地节点
     *
     * @param localHost 本地主机地址
     * @param localPort 本地 Dubbo 端口
     */
    public boolean isLocal(String localHost, int localPort) {
        return Objects.equals(this.host, localHost) && this.port == localPort;
    }

    /**
     * 该节点是否托管指定 ActorSystem
     */
    public boolean hasActorSystem(String actorSystemName) {
        return actorSystemNames.contains(actorSystemName);
    }

    @Override
    public String toString() {
        return "ClusterNode{" + nodeId + ", systems=" + actorSystemNames + "}";
    }
}
