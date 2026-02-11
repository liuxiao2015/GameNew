package com.game.actor.config;

import com.game.actor.cluster.ActorClusterConfig;
import com.game.actor.cluster.ActorClusterListener;
import com.game.actor.cluster.ActorShardRouter;
import com.game.api.actor.ActorRemoteService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actor 集群分片自动配置
 * <p>
 * 当 {@code game.actor.cluster.enabled=true} 时自动创建:
 * <ul>
 *     <li>{@link ActorClusterConfig} -- 集群配置</li>
 *     <li>{@link ActorShardRouter} -- 分片路由器</li>
 *     <li>{@link ActorClusterListener} -- 拓扑监听器</li>
 * </ul>
 * </p>
 * <p>
 * 未启用时不创建任何 Bean, 完全零侵入。
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "game.actor.cluster", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ActorClusterConfig.class)
public class ActorClusterAutoConfiguration {

    @Value("${dubbo.protocol.port:20880}")
    private int dubboPort;

    @Value("${server.address:#{null}}")
    private String serverAddress;

    @Autowired
    private ActorClusterConfig config;

    @Autowired(required = false)
    private ActorRemoteService actorRemoteService;

    @Bean
    public ActorShardRouter actorShardRouter() {
        ActorShardRouter router = new ActorShardRouter(config);

        // 注入 Dubbo 远程服务
        if (actorRemoteService != null) {
            router.setRemoteService(actorRemoteService);
        }

        // 设置本地节点信息
        String localHost = resolveLocalHost();
        router.setLocalNode(localHost, dubboPort);

        log.info("ActorShardRouter 创建: localNode={}:{}, virtualNodes={}",
                localHost, dubboPort, config.getVirtualNodes());
        return router;
    }

    @Bean
    public ActorClusterListener actorClusterListener(ActorShardRouter shardRouter) {
        ActorClusterListener listener = new ActorClusterListener(shardRouter, config);
        return listener;
    }

    @PostConstruct
    public void init() {
        log.info("Actor 集群分片已启用: virtualNodes={}, autoMigrate={}, refreshInterval={}s",
                config.getVirtualNodes(), config.isAutoMigrate(), config.getRefreshIntervalSeconds());
    }

    @PreDestroy
    public void destroy() {
        // ActorClusterListener.stop() 由 Spring 管理
    }

    /**
     * 解析本地主机地址
     */
    private String resolveLocalHost() {
        if (serverAddress != null && !serverAddress.isEmpty()) {
            return serverAddress;
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("无法解析本地主机地址, 使用 127.0.0.1", e);
            return "127.0.0.1";
        }
    }
}
