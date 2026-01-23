package com.game.common.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;

/**
 * 随机工具类
 * <p>
 * 游戏常用的随机和概率计算
 * </p>
 *
 * @author GameServer
 */
public final class RandomUtil {

    private RandomUtil() {
        // 禁止实例化
    }

    // ==================== 基础随机 ====================

    /**
     * 获取 [0, max) 范围内的随机整数
     */
    public static int nextInt(int max) {
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * 获取 [min, max) 范围内的随机整数
     */
    public static int nextInt(int min, int max) {
        if (min >= max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    /**
     * 获取 [min, max] 范围内的随机整数 (包含 max)
     */
    public static int nextIntClosed(int min, int max) {
        return nextInt(min, max + 1);
    }

    /**
     * 获取 [0, max) 范围内的随机长整数
     */
    public static long nextLong(long max) {
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextLong(max);
    }

    /**
     * 获取 [min, max) 范围内的随机长整数
     */
    public static long nextLong(long min, long max) {
        if (min >= max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    /**
     * 获取 [0.0, 1.0) 范围内的随机浮点数
     */
    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * 获取 [0.0, max) 范围内的随机浮点数
     */
    public static double nextDouble(double max) {
        return ThreadLocalRandom.current().nextDouble(max);
    }

    // ==================== 概率判断 ====================

    /**
     * 概率命中判断 (万分比)
     *
     * @param rate 概率值 (0-10000)
     * @return true=命中
     */
    public static boolean hit(int rate) {
        if (rate <= 0) {
            return false;
        }
        if (rate >= 10000) {
            return true;
        }
        return nextInt(10000) < rate;
    }

    /**
     * 概率命中判断 (百分比)
     *
     * @param percent 概率值 (0-100)
     * @return true=命中
     */
    public static boolean hitPercent(int percent) {
        return hit(percent * 100);
    }

    /**
     * 概率命中判断 (浮点数，0.0-1.0)
     *
     * @param probability 概率值 (0.0-1.0)
     * @return true=命中
     */
    public static boolean hitProbability(double probability) {
        if (probability <= 0) {
            return false;
        }
        if (probability >= 1.0) {
            return true;
        }
        return nextDouble() < probability;
    }

    // ==================== 权重随机 ====================

    /**
     * 权重随机选择
     *
     * @param items        元素列表
     * @param weightGetter 权重获取函数
     * @param <T>          元素类型
     * @return 随机选中的元素，如果列表为空返回 null
     */
    public static <T> T randomByWeight(List<T> items, ToIntFunction<T> weightGetter) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        if (items.size() == 1) {
            return items.get(0);
        }

        // 计算总权重
        int totalWeight = 0;
        for (T item : items) {
            totalWeight += weightGetter.applyAsInt(item);
        }

        if (totalWeight <= 0) {
            return items.get(nextInt(items.size()));
        }

        // 随机选择
        int random = nextInt(totalWeight);
        int currentWeight = 0;

        for (T item : items) {
            currentWeight += weightGetter.applyAsInt(item);
            if (random < currentWeight) {
                return item;
            }
        }

        return items.get(items.size() - 1);
    }

    /**
     * 权重随机选择多个 (不重复)
     *
     * @param items        元素列表
     * @param weightGetter 权重获取函数
     * @param count        选择数量
     * @param <T>          元素类型
     * @return 随机选中的元素列表
     */
    public static <T> List<T> randomByWeight(List<T> items, ToIntFunction<T> weightGetter, int count) {
        if (items == null || items.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        if (count >= items.size()) {
            return new ArrayList<>(items);
        }

        List<T> pool = new ArrayList<>(items);
        List<T> result = new ArrayList<>(count);

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            T selected = randomByWeight(pool, weightGetter);
            if (selected != null) {
                result.add(selected);
                pool.remove(selected);
            }
        }

        return result;
    }

    // ==================== 列表随机 ====================

    /**
     * 从列表中随机选择一个元素
     */
    public static <T> T randomOne(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(nextInt(list.size()));
    }

    /**
     * 从数组中随机选择一个元素
     */
    @SafeVarargs
    public static <T> T randomOne(T... array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[nextInt(array.length)];
    }

    /**
     * 从列表中随机选择多个元素 (不重复)
     */
    public static <T> List<T> randomN(List<T> list, int count) {
        if (list == null || list.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        if (count >= list.size()) {
            List<T> result = new ArrayList<>(list);
            Collections.shuffle(result);
            return result;
        }

        List<T> pool = new ArrayList<>(list);
        Collections.shuffle(pool);
        return pool.subList(0, count);
    }

    /**
     * 打乱列表顺序
     */
    public static <T> void shuffle(List<T> list) {
        if (list != null && list.size() > 1) {
            Collections.shuffle(list, ThreadLocalRandom.current());
        }
    }

    /**
     * 返回打乱后的新列表
     */
    public static <T> List<T> shuffled(List<T> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(list);
        shuffle(result);
        return result;
    }

    // ==================== 范围随机 ====================

    /**
     * 在给定范围内随机 (支持浮动)
     *
     * @param base  基础值
     * @param range 浮动范围 (如 0.1 表示 ±10%)
     * @return 随机值
     */
    public static int randomRange(int base, double range) {
        if (range <= 0) {
            return base;
        }
        int delta = (int) (base * range);
        return base + nextInt(-delta, delta + 1);
    }

    /**
     * 在给定范围内随机 (绝对值)
     *
     * @param base  基础值
     * @param delta 浮动绝对值
     * @return 随机值
     */
    public static int randomRange(int base, int delta) {
        if (delta <= 0) {
            return base;
        }
        return base + nextInt(-delta, delta + 1);
    }
}
