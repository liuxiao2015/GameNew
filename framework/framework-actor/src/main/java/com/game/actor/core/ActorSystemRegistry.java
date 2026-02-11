package com.game.actor.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActorSystem 注册表
 * <p>
 * 维护所有命名 ActorSystem 的全局索引，供远程 Actor 消息投递等场景使用。
 * ActorSystem 创建后自动注册，销毁后自动注销。
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class ActorSystemRegistry {

    private static final Map<String, ActorSystem<?>> REGISTRY = new ConcurrentHashMap<>();

    /**
     * 注册 ActorSystem
     */
    public static void register(ActorSystem<?> actorSystem) {
        ActorSystem<?> prev = REGISTRY.put(actorSystem.getName(), actorSystem);
        if (prev != null) {
            log.warn("ActorSystem 重复注册, 已覆盖: name={}", actorSystem.getName());
        }
        log.info("ActorSystem 注册: name={}", actorSystem.getName());
    }

    /**
     * 注销 ActorSystem
     */
    public static void unregister(String name) {
        REGISTRY.remove(name);
        log.info("ActorSystem 注销: name={}", name);
    }

    /**
     * 根据名称获取 ActorSystem
     */
    @SuppressWarnings("unchecked")
    public static <T extends Actor<?>> ActorSystem<T> get(String name) {
        return (ActorSystem<T>) REGISTRY.get(name);
    }

    /**
     * 获取所有注册的 ActorSystem
     */
    public static Collection<ActorSystem<?>> getAll() {
        return REGISTRY.values();
    }

    /**
     * 获取所有注册的 ActorSystem 名称
     */
    public static Set<String> getNames() {
        return new LinkedHashSet<>(REGISTRY.keySet());
    }

    /**
     * 获取注册数量
     */
    public static int size() {
        return REGISTRY.size();
    }
}
