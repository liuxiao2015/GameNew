package com.game.core.config.game;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 配置容器基类
 * <p>
 * 提供配置数据的存储和查询能力
 * </p>
 *
 * @param <T> 配置类型
 * @author GameServer
 */
@Slf4j
public abstract class BaseConfigContainer<T extends GameConfig> {

    /**
     * ID -> 配置映射
     */
    private final Map<Integer, T> configMap = new ConcurrentHashMap<>();

    /**
     * 所有配置列表
     */
    @Getter
    private volatile List<T> allList = Collections.emptyList();

    /**
     * 是否已加载
     */
    @Getter
    private volatile boolean loaded = false;

    /**
     * 加载时间
     */
    @Getter
    private long loadTime;

    /**
     * 根据 ID 获取配置
     *
     * @param id 配置 ID
     * @return 配置，不存在返回 null
     */
    public T get(int id) {
        return configMap.get(id);
    }

    /**
     * 根据 ID 获取配置 (不存在抛异常)
     *
     * @param id 配置 ID
     * @return 配置
     * @throws IllegalArgumentException 配置不存在
     */
    public T getOrThrow(int id) {
        T config = configMap.get(id);
        if (config == null) {
            throw new IllegalArgumentException(
                    String.format("配置不存在: %s, id=%d", getClass().getSimpleName(), id));
        }
        return config;
    }

    /**
     * 检查配置是否存在
     */
    public boolean contains(int id) {
        return configMap.containsKey(id);
    }

    /**
     * 获取所有配置
     */
    public Collection<T> getAll() {
        return allList;
    }

    /**
     * 获取配置数量
     */
    public int size() {
        return configMap.size();
    }

    /**
     * 按条件查找
     */
    public List<T> findAll(Predicate<T> predicate) {
        return allList.stream().filter(predicate).toList();
    }

    /**
     * 按条件查找第一个
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return allList.stream().filter(predicate).findFirst();
    }

    /**
     * 加载配置数据 (由 ConfigLoader 调用)
     */
    public void load(List<T> configs) {
        Map<Integer, T> newMap = new HashMap<>();
        List<T> newList = new ArrayList<>();

        for (T config : configs) {
            // 调用 afterLoad
            config.afterLoad();

            // 校验
            String error = config.validate();
            if (error != null && !error.isEmpty()) {
                log.error("配置校验失败: {}, id={}, error={}",
                        getClass().getSimpleName(), config.getId(), error);
                continue;
            }

            newMap.put(config.getId(), config);
            newList.add(config);
        }

        // 原子更新
        configMap.clear();
        configMap.putAll(newMap);
        allList = Collections.unmodifiableList(newList);
        loaded = true;
        loadTime = System.currentTimeMillis();

        log.info("加载配置: {}, count={}", getClass().getSimpleName(), size());

        // 回调
        afterLoad();
    }

    /**
     * 配置加载后的回调 (子类可覆盖)
     */
    protected void afterLoad() {
        // 默认空实现
    }

    /**
     * 重新加载 (热更新)
     */
    public void reload(List<T> configs) {
        load(configs);
        log.info("热更新配置: {}, count={}", getClass().getSimpleName(), size());
    }

    /**
     * 清空配置
     */
    public void clear() {
        configMap.clear();
        allList = Collections.emptyList();
        loaded = false;
    }
}
