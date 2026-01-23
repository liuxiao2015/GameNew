package com.game.common.math;

/**
 * 数学工具类
 * <p>
 * 提供溢出检测、安全运算等功能
 * </p>
 *
 * @author GameServer
 */
public final class MathUtil {

    private MathUtil() {
    }

    // ==================== 溢出检测 ====================

    /**
     * 检测加法是否会溢出
     *
     * @return true 表示会溢出
     */
    public static boolean addWillOverflow(long a, long b) {
        long result = a + b;
        // 两个正数相加得负数，或两个负数相加得正数，说明溢出
        return ((a ^ result) & (b ^ result)) < 0;
    }

    /**
     * 检测减法是否会溢出
     *
     * @return true 表示会溢出
     */
    public static boolean subtractWillOverflow(long a, long b) {
        long result = a - b;
        // a 和 b 符号不同，且结果和 a 符号不同
        return ((a ^ b) & (a ^ result)) < 0;
    }

    /**
     * 检测乘法是否会溢出
     *
     * @return true 表示会溢出
     */
    public static boolean multiplyWillOverflow(long a, long b) {
        if (a == 0 || b == 0) {
            return false;
        }
        long result = a * b;
        return result / a != b;
    }

    // ==================== 安全运算 (返回 Long128) ====================

    /**
     * 安全加法，如果溢出则返回 Long128
     */
    public static Long128 safeAdd(long a, long b) {
        if (!addWillOverflow(a, b)) {
            return Long128.valueOf(a + b);
        }
        return Long128.valueOf(a).add(b);
    }

    /**
     * 安全减法，如果溢出则返回 Long128
     */
    public static Long128 safeSubtract(long a, long b) {
        if (!subtractWillOverflow(a, b)) {
            return Long128.valueOf(a - b);
        }
        return Long128.valueOf(a).subtract(b);
    }

    /**
     * 安全乘法，如果溢出则返回 Long128
     */
    public static Long128 safeMultiply(long a, long b) {
        if (!multiplyWillOverflow(a, b)) {
            return Long128.valueOf(a * b);
        }
        return Long128.valueOf(a).multiply(b);
    }

    /**
     * 多个 long 安全求和
     */
    public static Long128 safeSum(long... values) {
        return Long128.sumLongs(values);
    }

    // ==================== 带溢出处理的运算 ====================

    /**
     * 加法，溢出时返回 Long.MAX_VALUE
     */
    public static long addWithCap(long a, long b) {
        if (addWillOverflow(a, b)) {
            return a > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return a + b;
    }

    /**
     * 加法，溢出时抛出异常
     */
    public static long addExact(long a, long b) {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0) {
            throw new ArithmeticException("long overflow: " + a + " + " + b);
        }
        return result;
    }

    /**
     * 乘法，溢出时返回 Long.MAX_VALUE
     */
    public static long multiplyWithCap(long a, long b) {
        if (multiplyWillOverflow(a, b)) {
            return (a > 0 == b > 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return a * b;
    }

    /**
     * 乘法，溢出时抛出异常
     */
    public static long multiplyExact(long a, long b) {
        long result = a * b;
        if (a != 0 && result / a != b) {
            throw new ArithmeticException("long overflow: " + a + " * " + b);
        }
        return result;
    }

    // ==================== 范围限制 ====================

    /**
     * 将值限制在指定范围内
     */
    public static long clamp(long value, long min, long max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * 将值限制在 0 到 Long.MAX_VALUE 范围内
     */
    public static long clampPositive(long value) {
        return Math.max(0, value);
    }

    // ==================== 百分比计算 ====================

    /**
     * 计算百分比（避免溢出）
     *
     * @param value   原值
     * @param percent 百分比 (100 = 100%)
     * @return value * percent / 100
     */
    public static long percent(long value, int percent) {
        if (percent == 100) {
            return value;
        }
        if (percent == 0) {
            return 0;
        }

        // 检测是否会溢出
        if (multiplyWillOverflow(value, percent)) {
            return Long128.valueOf(value).multiply(percent).divide(100).toLong();
        }
        return value * percent / 100;
    }

    /**
     * 计算万分比（避免溢出）
     *
     * @param value   原值
     * @param permil  万分比 (10000 = 100%)
     * @return value * permil / 10000
     */
    public static long permil(long value, int permil) {
        if (permil == 10000) {
            return value;
        }
        if (permil == 0) {
            return 0;
        }

        if (multiplyWillOverflow(value, permil)) {
            return Long128.valueOf(value).multiply(permil).divide(10000).toLong();
        }
        return value * permil / 10000;
    }

    // ==================== 格式化 ====================

    /**
     * 格式化大数字为带单位的字符串
     * <p>
     * 例如: 12345678 -> "1234.57万"
     *       1234567890 -> "12.35亿"
     * </p>
     */
    public static String formatNumber(long value) {
        if (Math.abs(value) < 10000) {
            return String.valueOf(value);
        }
        if (Math.abs(value) < 100000000) {
            return String.format("%.2f万", value / 10000.0);
        }
        return String.format("%.2f亿", value / 100000000.0);
    }

    /**
     * 格式化 Long128 为带单位的字符串
     */
    public static String formatNumber(Long128 value) {
        if (value.fitsInLong()) {
            return formatNumber(value.toLong());
        }
        // 超大数字
        double doubleValue = value.toDouble();
        if (doubleValue < 1e12) {
            return String.format("%.2f亿", doubleValue / 1e8);
        }
        if (doubleValue < 1e16) {
            return String.format("%.2f万亿", doubleValue / 1e12);
        }
        return String.format("%.2e", doubleValue);
    }
}
