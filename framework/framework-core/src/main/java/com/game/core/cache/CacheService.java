package com.game.core.cache;

import com.game.data.redis.RedisService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 二级缓存服务
 * <p>
 * 结合本地缓存 (Caffeine) 和分布式缓存 (Redis)，提供高性能缓存：
 * <ul>
 *     <li>L1: 本地 Caffeine 缓存，极速访问</li>
 *     <li>L2: Redis 分布式缓存，跨机器共享</li>
 *     <li>自动穿透加载</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 获取或加载数据
 * PlayerConfig config = cacheService.get("player_config", playerId,
 *     id -> playerConfigRepository.findById(id).orElse(null),
 *     PlayerConfig.class);
 *
 * // 直接设置缓存
 * cacheService.put("player_config", playerId, config);
 *
 * // 删除缓存
 * cacheService.evict("player_config", playerId);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisService redisService;

    /**
     * 本地缓存容器 (cacheName -> Cache)
     */
    private final Map<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();

    /**
     * Redis Key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "cache:";

    /**
     * 默认本地缓存配置
     */
    private static final int DEFAULT_LOCAL_MAX_SIZE = 10000;
    private static final Duration DEFAULT_LOCAL_EXPIRE = Duration.ofMinutes(5);

    /**
     * 默认 Redis 缓存过期时间
     */
    private static final Duration DEFAULT_REDIS_EXPIRE = Duration.ofMinutes(30);

    @PostConstruct
    public void init() {
        log.info("CacheService 初始化完成");
    }

    // ==================== 获取缓存 ====================

    /**
     * 获取缓存 (优先本地缓存，其次 Redis，最后加载)
     *
     * @param cacheName 缓存名称
     * @param key       缓存 Key
     * @param loader    加载函数
     * @param clazz     值类型
     * @param <T>       值类型
     * @return 缓存值
     */
    public <T> T get(String cacheName, Object key, Function<Object, T> loader, Class<T> clazz) {
        String cacheKey = buildKey(key);

        // 1. 查本地缓存
        Cache<String, Object> localCache = getOrCreateLocalCache(cacheName);
        Object localValue = localCache.getIfPresent(cacheKey);
        if (localValue != null) {
            return clazz.cast(localValue);
        }

        // 2. 查 Redis
        String redisKey = buildRedisKey(cacheName, cacheKey);
        T redisValue = redisService.getObject(redisKey, clazz);
        if (redisValue != null) {
            // 回填本地缓存
            localCache.put(cacheKey, redisValue);
            return redisValue;
        }

        // 3. 加载数据
        if (loader != null) {
            T loadedValue = loader.apply(key);
            if (loadedValue != null) {
                put(cacheName, key, loadedValue);
                return loadedValue;
            }
        }

        return null;
    }

    /**
     * 获取缓存 (只查缓存，不加载)
     */
    public <T> T getIfPresent(String cacheName, Object key, Class<T> clazz) {
        return get(cacheName, key, null, clazz);
    }

    /**
     * 获取缓存 (简化版 - 用于单 key 场景)
     * 
     * @param key      缓存 Key
     * @param supplier 加载函数 (懒加载)
     * @param expire   过期时间
     * @param <T>      值类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, java.util.function.Supplier<T> supplier, java.time.Duration expire) {
        // 查本地缓存
        Cache<String, Object> localCache = getOrCreateLocalCache("default");
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            return (T) localValue;
        }

        // 查 Redis
        String redisKey = CACHE_KEY_PREFIX + key;
        String redisValue = redisService.get(redisKey);
        if (redisValue != null) {
            try {
                T value = (T) com.game.common.util.JsonUtil.fromJson(redisValue, Object.class);
                localCache.put(key, value);
                return value;
            } catch (Exception e) {
                log.warn("缓存反序列化失败: key={}", key, e);
            }
        }

        // 加载数据
        if (supplier != null) {
            T loadedValue = supplier.get();
            if (loadedValue != null) {
                localCache.put(key, loadedValue);
                redisService.set(redisKey, com.game.common.util.JsonUtil.toJson(loadedValue), expire);
                return loadedValue;
            }
        }

        return null;
    }

    // ==================== 设置缓存 ====================

    /**
     * 设置缓存 (同时更新本地和 Redis)
     */
    public void put(String cacheName, Object key, Object value) {
        put(cacheName, key, value, DEFAULT_REDIS_EXPIRE);
    }

    /**
     * 设置缓存 (指定 Redis 过期时间)
     */
    public void put(String cacheName, Object key, Object value, Duration redisExpire) {
        if (value == null) {
            return;
        }

        String cacheKey = buildKey(key);

        // 更新本地缓存
        Cache<String, Object> localCache = getOrCreateLocalCache(cacheName);
        localCache.put(cacheKey, value);

        // 更新 Redis
        String redisKey = buildRedisKey(cacheName, cacheKey);
        redisService.setObject(redisKey, value, redisExpire);
    }

    // ==================== 删除缓存 ====================

    /**
     * 删除缓存 (同时删除本地和 Redis)
     */
    public void evict(String cacheName, Object key) {
        String cacheKey = buildKey(key);

        // 删除本地缓存
        Cache<String, Object> localCache = localCaches.get(cacheName);
        if (localCache != null) {
            localCache.invalidate(cacheKey);
        }

        // 删除 Redis
        String redisKey = buildRedisKey(cacheName, cacheKey);
        redisService.delete(redisKey);
    }

    /**
     * 清空指定缓存（本地 + Redis 前缀删除）
     */
    public void evictAll(String cacheName) {
        // 清空本地缓存
        Cache<String, Object> localCache = localCaches.get(cacheName);
        if (localCache != null) {
            localCache.invalidateAll();
        }
        // 注意: Redis 按前缀删除需要谨慎，这里只清空本地
        log.info("清空缓存: {}", cacheName);
    }

    /**
     * 清空指定缓存的本地缓存
     */
    public void evictLocal(String cacheName) {
        Cache<String, Object> localCache = localCaches.get(cacheName);
        if (localCache != null) {
            localCache.invalidateAll();
            log.info("清空本地缓存: {}", cacheName);
        }
    }

    /**
     * 清空所有本地缓存
     */
    public void evictAllLocal() {
        localCaches.values().forEach(Cache::invalidateAll);
        log.info("清空所有本地缓存");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取或创建本地缓存
     */
    private Cache<String, Object> getOrCreateLocalCache(String cacheName) {
        return localCaches.computeIfAbsent(cacheName, name ->
                Caffeine.newBuilder()
                        .maximumSize(DEFAULT_LOCAL_MAX_SIZE)
                        .expireAfterWrite(DEFAULT_LOCAL_EXPIRE)
                        .recordStats()  // 启用统计
                        .build()
        );
    }

    /**
     * 创建自定义配置的本地缓存
     */
    public void createCache(String cacheName, int maxSize, Duration expire) {
        localCaches.put(cacheName,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(expire)
                        .build()
        );
        log.info("创建本地缓存: name={}, maxSize={}, expire={}", cacheName, maxSize, expire);
    }

    private String buildKey(Object key) {
        return String.valueOf(key);
    }

    private String buildRedisKey(String cacheName, String cacheKey) {
        return CACHE_KEY_PREFIX + cacheName + ":" + cacheKey;
    }

    /**
     * 获取本地缓存统计
     */
    public Map<String, CacheStats> getStats() {
        Map<String, CacheStats> stats = new ConcurrentHashMap<>();
        localCaches.forEach((name, cache) -> {
            stats.put(name, new CacheStats(
                    cache.estimatedSize(),
                    cache.stats().hitCount(),
                    cache.stats().missCount()
            ));
        });
        return stats;
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(long size, long hitCount, long missCount) {
        public double hitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0;
        }
    }
}
