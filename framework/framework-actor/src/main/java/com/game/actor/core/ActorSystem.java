package com.game.actor.core;

import com.game.actor.cluster.ActorShardRouter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Actor 系统
 * <p>
 * 管理 Actor 的生命周期、调度和持久化。
 * 增强: 全局监督策略 + 异常上报回调。
 * </p>
 *
 * @param <T> Actor 类型
 * @author GameServer
 */
@Slf4j
public class ActorSystem<T extends Actor<?>> {

    @Getter
    private final String name;

    private final Cache<Long, T> actorCache;

    private final Function<Long, T> actorFactory;

    /**
     * 执行器 (使用虚拟线程)
     */
    private final ExecutorService executor;

    /**
     * 定时任务调度器
     */
    private final ScheduledExecutorService scheduler;

    private final ActorSystemConfig config;

    /**
     * 全局监督策略
     */
    @Getter
    private final SupervisorStrategy supervisorStrategy;

    /**
     * 异常上报处理器 (ESCALATE 指令的回调)
     */
    private final BiConsumer<Actor<?>, Throwable> escalateHandler;

    /**
     * 集群分片路由器 (可选)
     * <p>
     * 非 null 时 {@link #tellCluster(long, ActorMessage)} 方法将自动路由到正确的节点。
     * 由 Spring 注入，未配置集群时为 null。
     * </p>
     */
    @Setter
    private ActorShardRouter shardRouter;

    private volatile boolean shutdown = false;

    public ActorSystem(String name, ActorSystemConfig config, Function<Long, T> actorFactory) {
        this.name = name;
        this.config = config;
        this.actorFactory = actorFactory;
        this.supervisorStrategy = config.getSupervisorStrategy();
        this.escalateHandler = config.getEscalateHandler();

        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, name + "-scheduler");
            t.setDaemon(true);
            return t;
        });

        this.actorCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterAccess(Duration.ofMinutes(config.getIdleTimeoutMinutes()))
                .removalListener((Long key, T actor, RemovalCause cause) -> {
                    if (actor != null && cause != RemovalCause.REPLACED) {
                        onActorRemoval(key, actor, cause);
                    }
                })
                .build();
    }

    @PostConstruct
    public void init() {
        long saveIntervalSeconds = config.getSaveIntervalSeconds();
        scheduler.scheduleAtFixedRate(
                this::saveAllActors,
                saveIntervalSeconds,
                saveIntervalSeconds,
                TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
                this::checkIdleActors,
                60,
                60,
                TimeUnit.SECONDS
        );

        // 注册到全局注册表 (供远程 Actor 消息路由)
        ActorSystemRegistry.register(this);

        log.info("ActorSystem 启动成功: name={}, maxSize={}, idleTimeout={}min, saveInterval={}s",
                name, config.getMaxSize(), config.getIdleTimeoutMinutes(), saveIntervalSeconds);
    }

    @PreDestroy
    public void destroy() {
        shutdown = true;
        log.info("ActorSystem 开始关闭: name={}", name);

        scheduler.shutdown();
        saveAllActors();
        actorCache.asMap().values().forEach(Actor::stop);
        actorCache.invalidateAll();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 从全局注册表注销
        ActorSystemRegistry.unregister(name);

        log.info("ActorSystem 关闭完成: name={}", name);
    }

    // ==================== Actor 管理 ====================

    public T getActor(long actorId) {
        if (shutdown) {
            return null;
        }
        return actorCache.get(actorId, id -> {
            T actor = actorFactory.apply(id);
            if (actor != null) {
                actor.setActorSystem(this);
                actor.start();
            }
            return actor;
        });
    }

    public T getActorIfPresent(long actorId) {
        return actorCache.getIfPresent(actorId);
    }

    // ==================== 消息发送 ====================

    public boolean tell(long actorId, ActorMessage message) {
        T actor = getActor(actorId);
        if (actor != null) {
            return actor.tell(message);
        }
        return false;
    }

    public boolean tell(long actorId, Object data) {
        return tell(actorId, ActorMessage.of("DATA", data));
    }

    public boolean tellIfPresent(long actorId, ActorMessage message) {
        T actor = getActorIfPresent(actorId);
        if (actor != null) {
            return actor.tell(message);
        }
        return false;
    }

    // ==================== 集群路由消息发送 ====================

    /**
     * 向 Actor 发送消息 (集群感知)
     * <p>
     * 自动判断目标 Actor 在本地还是远程:
     * <ul>
     *     <li>本地 → 直接投递到本地 Actor 邮箱</li>
     *     <li>远程 → 通过 Dubbo RPC 转发到目标节点</li>
     *     <li>无 ShardRouter → 退化为本地 tell()</li>
     * </ul>
     * </p>
     *
     * @param actorId 目标 Actor ID
     * @param message 消息
     * @return 是否成功投递
     */
    public boolean tellCluster(long actorId, ActorMessage message) {
        if (shardRouter == null) {
            // 未配置集群, 退化为本地
            return tell(actorId, message);
        }
        return shardRouter.tell(name, actorId, message);
    }

    /**
     * 向 Actor 发送消息 (集群感知, 便捷方法)
     */
    public boolean tellCluster(long actorId, Object data) {
        return tellCluster(actorId, ActorMessage.of("DATA", data));
    }

    /**
     * 判断指定 Actor 是否由本地节点管理
     *
     * @param actorId Actor ID
     * @return true 表示本地, false 表示远程; 无集群时总是返回 true
     */
    public boolean isLocalActor(long actorId) {
        if (shardRouter == null) {
            return true;
        }
        return shardRouter.isLocal(actorId);
    }

    // ==================== Actor 生命周期 ====================

    public void removeActor(long actorId) {
        T actor = actorCache.getIfPresent(actorId);
        if (actor != null) {
            actor.stop();
            actorCache.invalidate(actorId);
        }
    }

    public boolean hasActor(long actorId) {
        return actorCache.getIfPresent(actorId) != null;
    }

    public Collection<T> getAllActors() {
        return actorCache.asMap().values();
    }

    public long getActorCount() {
        return actorCache.estimatedSize();
    }

    // ==================== 监督策略支持 ====================

    /**
     * 当 Actor 使用 ESCALATE 指令时被调用
     */
    void onEscalate(Actor<?> actor, ActorMessage message, Exception error) {
        log.error("Actor 异常上报: actorId={}, type={}, msgType={}, error={}",
                actor.getActorId(), actor.getActorType(),
                message != null ? message.getType() : "null",
                error.getMessage(), error);

        if (escalateHandler != null) {
            try {
                escalateHandler.accept(actor, error);
            } catch (Exception handlerError) {
                log.error("ESCALATE 处理器自身异常: actorId={}", actor.getActorId(), handlerError);
            }
        }
    }

    // ==================== 内部方法 ====================

    void execute(Runnable task) {
        if (!shutdown) {
            executor.execute(task);
        }
    }

    private void onActorRemoval(Long actorId, T actor, RemovalCause cause) {
        log.info("Actor 被移除: actorId={}, cause={}", actorId, cause);
        try {
            actor.stop();
        } catch (Exception e) {
            log.error("Actor 移除处理异常: actorId={}", actorId, e);
        }
    }

    private void saveAllActors() {
        long saveIntervalMs = config.getSaveIntervalSeconds() * 1000L;
        actorCache.asMap().values().forEach(actor -> {
            try {
                actor.checkAndSave(saveIntervalMs);
            } catch (Exception e) {
                log.error("Actor 保存异常: actorId={}", actor.getActorId(), e);
            }
        });
    }

    private void checkIdleActors() {
        long idleTimeoutMs = config.getIdleTimeoutMinutes() * 60 * 1000L;
        long idleCount = actorCache.asMap().values().stream()
                .filter(actor -> actor.isIdle(idleTimeoutMs))
                .count();

        if (idleCount > 0) {
            log.debug("ActorSystem 空闲检查: name={}, total={}, idle={}",
                    name, actorCache.estimatedSize(), idleCount);
        }
    }

    // ==================== 配置 ====================

    @Getter
    public static class ActorSystemConfig {
        private int maxSize = 10000;
        private int idleTimeoutMinutes = 30;
        private int saveIntervalSeconds = 300;
        private SupervisorStrategy supervisorStrategy = SupervisorStrategy.defaultStrategy();
        private BiConsumer<Actor<?>, Throwable> escalateHandler;

        public ActorSystemConfig maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public ActorSystemConfig idleTimeoutMinutes(int minutes) {
            this.idleTimeoutMinutes = minutes;
            return this;
        }

        public ActorSystemConfig saveIntervalSeconds(int seconds) {
            this.saveIntervalSeconds = seconds;
            return this;
        }

        /**
         * 设置全局监督策略
         */
        public ActorSystemConfig supervisorStrategy(SupervisorStrategy strategy) {
            this.supervisorStrategy = strategy;
            return this;
        }

        /**
         * 设置 ESCALATE 回调处理器
         *
         * @param handler 接收 (Actor, Throwable) 的处理器
         */
        public ActorSystemConfig escalateHandler(BiConsumer<Actor<?>, Throwable> handler) {
            this.escalateHandler = handler;
            return this;
        }

        public static ActorSystemConfig create() {
            return new ActorSystemConfig();
        }
    }
}
