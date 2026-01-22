package com.game.actor.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Actor 系统
 * <p>
 * 管理 Actor 的生命周期、调度和持久化
 * </p>
 *
 * @param <T> Actor 类型
 * @author GameServer
 */
@Slf4j
public class ActorSystem<T extends Actor<?>> {

    /**
     * 系统名称
     */
    @Getter
    private final String name;

    /**
     * Actor 缓存
     */
    private final Cache<Long, T> actorCache;

    /**
     * Actor 创建工厂
     */
    private final Function<Long, T> actorFactory;

    /**
     * 执行器 (使用虚拟线程)
     */
    private final ExecutorService executor;

    /**
     * 定时任务调度器
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 配置
     */
    private final ActorSystemConfig config;

    /**
     * 是否已关闭
     */
    private volatile boolean shutdown = false;

    public ActorSystem(String name, ActorSystemConfig config, Function<Long, T> actorFactory) {
        this.name = name;
        this.config = config;
        this.actorFactory = actorFactory;

        // 创建虚拟线程执行器
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        // 创建调度器
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, name + "-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 创建 Caffeine 缓存
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
        // 启动定时保存任务
        long saveIntervalSeconds = config.getSaveIntervalSeconds();
        scheduler.scheduleAtFixedRate(
                this::saveAllActors,
                saveIntervalSeconds,
                saveIntervalSeconds,
                TimeUnit.SECONDS
        );

        // 启动空闲检查任务
        scheduler.scheduleAtFixedRate(
                this::checkIdleActors,
                60,
                60,
                TimeUnit.SECONDS
        );

        log.info("ActorSystem 启动成功: name={}, maxSize={}, idleTimeout={}min, saveInterval={}s",
                name, config.getMaxSize(), config.getIdleTimeoutMinutes(), saveIntervalSeconds);
    }

    @PreDestroy
    public void destroy() {
        shutdown = true;
        log.info("ActorSystem 开始关闭: name={}", name);

        // 停止调度器
        scheduler.shutdown();

        // 保存所有 Actor 数据
        saveAllActors();

        // 停止所有 Actor
        actorCache.asMap().values().forEach(Actor::stop);
        actorCache.invalidateAll();

        // 关闭执行器
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("ActorSystem 关闭完成: name={}", name);
    }

    /**
     * 获取或创建 Actor
     */
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

    /**
     * 获取 Actor (不创建)
     */
    public T getActorIfPresent(long actorId) {
        return actorCache.getIfPresent(actorId);
    }

    /**
     * 发送消息到 Actor
     */
    public boolean tell(long actorId, ActorMessage message) {
        T actor = getActor(actorId);
        if (actor != null) {
            return actor.tell(message);
        }
        return false;
    }

    /**
     * 发送数据消息到 Actor
     * <p>
     * 消息数据会被自动包装为 ActorMessage
     * </p>
     */
    public boolean tell(long actorId, Object data) {
        return tell(actorId, ActorMessage.of("DATA", data));
    }

    /**
     * 发送消息到 Actor (不自动创建)
     */
    public boolean tellIfPresent(long actorId, ActorMessage message) {
        T actor = getActorIfPresent(actorId);
        if (actor != null) {
            return actor.tell(message);
        }
        return false;
    }

    /**
     * 移除 Actor
     */
    public void removeActor(long actorId) {
        T actor = actorCache.getIfPresent(actorId);
        if (actor != null) {
            actor.stop();
            actorCache.invalidate(actorId);
        }
    }

    /**
     * 判断 Actor 是否存在
     */
    public boolean hasActor(long actorId) {
        return actorCache.getIfPresent(actorId) != null;
    }

    /**
     * 获取所有 Actor
     */
    public Collection<T> getAllActors() {
        return actorCache.asMap().values();
    }

    /**
     * 获取 Actor 数量
     */
    public long getActorCount() {
        return actorCache.estimatedSize();
    }

    /**
     * 执行 Actor 任务
     */
    void execute(Runnable task) {
        if (!shutdown) {
            executor.execute(task);
        }
    }

    /**
     * Actor 被移除时的回调
     */
    private void onActorRemoval(Long actorId, T actor, RemovalCause cause) {
        log.info("Actor 被移除: actorId={}, cause={}", actorId, cause);
        
        try {
            // 保存数据并停止
            actor.stop();
        } catch (Exception e) {
            log.error("Actor 移除处理异常: actorId={}", actorId, e);
        }
    }

    /**
     * 保存所有 Actor 数据
     */
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

    /**
     * 检查空闲 Actor
     */
    private void checkIdleActors() {
        // Caffeine 会自动处理过期，这里只做日志
        long idleTimeoutMs = config.getIdleTimeoutMinutes() * 60 * 1000L;
        long idleCount = actorCache.asMap().values().stream()
                .filter(actor -> actor.isIdle(idleTimeoutMs))
                .count();
        
        if (idleCount > 0) {
            log.debug("ActorSystem 空闲检查: name={}, total={}, idle={}", 
                    name, actorCache.estimatedSize(), idleCount);
        }
    }

    /**
     * ActorSystem 配置
     */
    @Getter
    public static class ActorSystemConfig {
        /**
         * 最大 Actor 数量
         */
        private int maxSize = 10000;

        /**
         * 空闲超时时间 (分钟)
         */
        private int idleTimeoutMinutes = 30;

        /**
         * 保存间隔 (秒)
         */
        private int saveIntervalSeconds = 300;

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

        public static ActorSystemConfig create() {
            return new ActorSystemConfig();
        }
    }
}
