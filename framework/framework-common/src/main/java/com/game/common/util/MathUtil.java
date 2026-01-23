package com.game.common.util;

/**
 * 数学工具类
 * <p>
 * 游戏常用的数学计算
 * </p>
 *
 * @author GameServer
 */
public final class MathUtil {

    private MathUtil() {
        // 禁止实例化
    }

    // ==================== 基础计算 ====================

    /**
     * 限制值在范围内
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制值在范围内
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制值在范围内
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 安全加法 (防止溢出)
     */
    public static int safeAdd(int a, int b) {
        long result = (long) a + b;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }

    /**
     * 安全加法 (防止溢出)
     */
    public static long safeAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            return a > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    /**
     * 安全乘法 (防止溢出)
     */
    public static int safeMultiply(int a, int b) {
        long result = (long) a * b;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }

    /**
     * 安全乘法 (防止溢出)
     */
    public static long safeMultiply(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return (a > 0) == (b > 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    // ==================== 百分比计算 ====================

    /**
     * 计算百分比 (万分比)
     *
     * @param base 基础值
     * @param rate 万分比 (10000 = 100%)
     * @return 计算结果
     */
    public static int percentRate(int base, int rate) {
        return (int) ((long) base * rate / 10000);
    }

    /**
     * 计算百分比 (万分比)
     */
    public static long percentRate(long base, int rate) {
        return base * rate / 10000;
    }

    /**
     * 增加百分比 (万分比)
     *
     * @param base 基础值
     * @param rate 增加的万分比
     * @return base + base * rate / 10000
     */
    public static int addPercent(int base, int rate) {
        return base + percentRate(base, rate);
    }

    /**
     * 增加百分比 (万分比)
     */
    public static long addPercent(long base, int rate) {
        return base + percentRate(base, rate);
    }

    /**
     * 减少百分比 (万分比)
     */
    public static int subPercent(int base, int rate) {
        return base - percentRate(base, rate);
    }

    /**
     * 浮点百分比
     */
    public static double percentFloat(double base, double percent) {
        return base * percent / 100.0;
    }

    // ==================== 游戏数值计算 ====================

    /**
     * 计算伤害 (考虑防御)
     * <p>
     * 伤害减免公式：实际伤害 = 攻击 * 攻击 / (攻击 + 防御)
     * </p>
     */
    public static int calcDamage(int attack, int defense) {
        if (attack <= 0) return 0;
        if (defense <= 0) return attack;
        return (int) ((long) attack * attack / (attack + defense));
    }

    /**
     * 计算暴击伤害
     */
    public static int calcCritDamage(int damage, int critRate) {
        // critRate: 万分比，15000 = 150% 暴击伤害
        return percentRate(damage, critRate);
    }

    /**
     * 计算经验等级
     * <p>
     * 升级经验公式：exp = baseExp * level ^ power
     * </p>
     */
    public static long calcLevelUpExp(int level, long baseExp, double power) {
        return (long) (baseExp * Math.pow(level, power));
    }

    /**
     * 计算战力
     */
    public static long calcCombatPower(int attack, int defense, int hp) {
        return (long) attack * 10 + (long) defense * 5 + hp;
    }

    // ==================== 距离计算 ====================

    /**
     * 计算两点距离 (曼哈顿距离)
     */
    public static int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * 计算两点距离 (欧几里得距离)
     */
    public static double euclideanDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两点距离平方 (避免开方，用于比较)
     */
    public static int distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    /**
     * 判断是否在范围内
     */
    public static boolean inRange(int x1, int y1, int x2, int y2, int range) {
        return distanceSquared(x1, y1, x2, y2) <= range * range;
    }

    // ==================== 区间判断 ====================

    /**
     * 判断值是否在区间内 (闭区间)
     */
    public static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * 判断值是否在区间内 (闭区间)
     */
    public static boolean inRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    // ==================== 向上取整除法 ====================

    /**
     * 向上取整除法
     */
    public static int ceilDiv(int dividend, int divisor) {
        if (divisor == 0) throw new ArithmeticException("Division by zero");
        return (dividend + divisor - 1) / divisor;
    }

    /**
     * 向上取整除法
     */
    public static long ceilDiv(long dividend, long divisor) {
        if (divisor == 0) throw new ArithmeticException("Division by zero");
        return (dividend + divisor - 1) / divisor;
    }

    // ==================== 对齐 ====================

    /**
     * 向上对齐到指定倍数
     */
    public static int alignUp(int value, int alignment) {
        return ceilDiv(value, alignment) * alignment;
    }

    /**
     * 向下对齐到指定倍数
     */
    public static int alignDown(int value, int alignment) {
        return (value / alignment) * alignment;
    }

    // ==================== 其他 ====================

    /**
     * 格式化大数字 (如: 1000 -> 1K, 1000000 -> 1M)
     */
    public static String formatNumber(long value) {
        if (value >= 1_000_000_000_000L) {
            return String.format("%.1fT", value / 1_000_000_000_000.0);
        }
        if (value >= 1_000_000_000) {
            return String.format("%.1fB", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    /**
     * 判断是否为 2 的幂
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /**
     * 计算比例分配
     */
    public static int[] distributeByRatio(int total, int... ratios) {
        int ratioSum = 0;
        for (int ratio : ratios) {
            ratioSum += ratio;
        }

        int[] result = new int[ratios.length];
        int remaining = total;

        for (int i = 0; i < ratios.length - 1; i++) {
            result[i] = total * ratios[i] / ratioSum;
            remaining -= result[i];
        }
        result[ratios.length - 1] = remaining; // 最后一个取余数

        return result;
    }
}
