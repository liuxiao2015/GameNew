package com.game.data.redis;

import com.game.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务封装
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // ==================== String 操作 ====================

    /**
     * 设置值
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值并设置过期时间
     */
    public void set(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 设置值并设置过期时间 (秒)
     */
    public void set(String key, String value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 设置对象 (JSON 序列化)
     */
    public void setObject(String key, Object value) {
        set(key, JsonUtil.toJson(value));
    }

    /**
     * 设置对象并设置过期时间
     */
    public void setObject(String key, Object value, Duration timeout) {
        set(key, JsonUtil.toJson(value), timeout);
    }

    /**
     * 获取值
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        String value = get(key);
        return value != null ? JsonUtil.fromJson(value, clazz) : null;
    }

    /**
     * 只有键不存在时才设置
     */
    public boolean setIfAbsent(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    /**
     * 只有键不存在时才设置 (带过期时间)
     */
    public boolean setIfAbsent(String key, String value, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    /**
     * 自增
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增指定值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    // ==================== Key 操作 ====================

    /**
     * 删除键
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除键
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断键是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    /**
     * 设置过期时间 (秒)
     */
    public boolean expire(String key, long timeoutSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS));
    }

    /**
     * 获取剩余过期时间 (秒)
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== Hash 操作 ====================

    /**
     * Hash 设置字段
     */
    public void hSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash 设置对象字段
     */
    public void hSetObject(String key, String field, Object value) {
        hSet(key, field, JsonUtil.toJson(value));
    }

    /**
     * Hash 批量设置
     */
    public void hSetAll(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * Hash 获取字段
     */
    public String hGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    /**
     * Hash 获取对象字段
     */
    public <T> T hGetObject(String key, String field, Class<T> clazz) {
        String value = hGet(key, field);
        return value != null ? JsonUtil.fromJson(value, clazz) : null;
    }

    /**
     * Hash 获取所有字段
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * Hash 删除字段
     */
    public Long hDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * Hash 判断字段是否存在
     */
    public boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * Hash 字段自增
     */
    public Long hIncrement(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    // ==================== Set 操作 ====================

    /**
     * Set 添加成员
     */
    public Long sAdd(String key, String... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Set 移除成员
     */
    public Long sRemove(String key, String... values) {
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }

    /**
     * Set 判断成员是否存在
     */
    public boolean sIsMember(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * Set 获取所有成员
     */
    public Set<String> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Set 获取成员数量
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // ==================== ZSet (Sorted Set) 操作 ====================

    /**
     * ZSet 添加成员
     */
    public boolean zAdd(String key, String member, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, member, score));
    }

    /**
     * ZSet 移除成员
     */
    public Long zRemove(String key, String... members) {
        return redisTemplate.opsForZSet().remove(key, (Object[]) members);
    }

    /**
     * ZSet 增加分数
     */
    public Double zIncrementScore(String key, String member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    /**
     * ZSet 获取成员排名 (从0开始，分数从低到高)
     */
    public Long zRank(String key, String member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    /**
     * ZSet 获取成员排名 (从0开始，分数从高到低)
     */
    public Long zReverseRank(String key, String member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * ZSet 获取成员分数
     */
    public Double zScore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * ZSet 获取排名范围的成员 (分数从高到低)
     */
    public Set<String> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * ZSet 获取排名范围的成员和分数 (分数从高到低)
     */
    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * ZSet 获取成员数量
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    // ==================== List 操作 ====================

    /**
     * List 左侧添加
     */
    public Long lLeftPush(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * List 右侧添加
     */
    public Long lRightPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List 左侧弹出
     */
    public String lLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * List 右侧弹出
     */
    public String lRightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * List 获取范围
     */
    public List<String> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * List 获取长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁
     */
    public boolean tryLock(String lockKey, String requestId, Duration timeout) {
        return setIfAbsent(lockKey, requestId, timeout);
    }

    /**
     * 释放分布式锁
     */
    public boolean releaseLock(String lockKey, String requestId) {
        String value = get(lockKey);
        if (requestId.equals(value)) {
            return delete(lockKey);
        }
        return false;
    }

    // ==================== 额外方法 ====================

    /**
     * 判断键是否存在 (别名)
     */
    public boolean exists(String key) {
        return hasKey(key);
    }

    /**
     * 设置值并设置过期时间 (秒) - 别名
     */
    public void setEx(String key, String value, long timeoutSeconds) {
        set(key, value, timeoutSeconds);
    }

    /**
     * Set 获取成员数量 (别名)
     */
    public Long sCard(String key) {
        return sSize(key);
    }

    /**
     * ZSet 获取排名范围的成员 (分数从低到高)
     */
    public Set<String> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * ZSet 获取成员数量 (别名)
     */
    public Long zCard(String key) {
        return zSize(key);
    }

    /**
     * ZSet 移除排名范围的成员
     */
    public Long zRemoveRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 发布消息到频道
     */
    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }
}
