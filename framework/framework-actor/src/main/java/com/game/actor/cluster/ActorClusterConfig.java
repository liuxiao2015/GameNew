package com.game.actor.cluster;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Actor 集群分片配置
 * <p>
 * 通过 application.yml 配置:
 * <pre>
 * game:
 *   actor:
 *     cluster:
 *       enabled: true
 *       virtual-nodes: 160
 *       auto-migrate: false
 *       service-name: ${spring.application.name}
 * </pre>
 * </p>
 *
 * @author GameServer
 */
@Data
@ConfigurationProperties(prefix = "game.actor.cluster")
public class ActorClusterConfig {

    /**
     * 是否启用集群分片
     * <p>
     * false 时所有 Actor 操作退化为本地模式, 零侵入。
     * </p>
     */
    private boolean enabled = false;

    /**
     * 一致性哈希环的虚拟节点数 (每个物理节点)
     * <p>
     * 值越大分布越均匀，建议 >= 100。默认 160，与 Dubbo ConsistentHash 一致。
     * </p>
     */
    private int virtualNodes = 160;

    /**
     * 是否启用节点变更时自动迁移 Actor
     * <p>
     * false (推荐): 依赖一致性哈希的自然特性，旧节点 Actor 通过 idle 超时回收，
     * 新请求在新节点自动创建 (从 DB 加载)。简单可靠。
     * <br/>
     * true: 节点变更时主动将受影响的 Actor 状态迁移到新节点 (复杂, 暂不实现)。
     * </p>
     */
    private boolean autoMigrate = false;

    /**
     * 服务名称 (用于 Nacos 实例订阅)
     * <p>
     * 默认使用 ${spring.application.name}，同一服务名下的多个实例组成一个集群。
     * </p>
     */
    private String serviceName;

    /**
     * 集群拓扑刷新间隔 (秒)
     * <p>
     * 除了 Nacos 事件推送外，额外的定时全量刷新周期。
     * 用于兜底，防止事件丢失。
     * </p>
     */
    private int refreshIntervalSeconds = 30;
}
