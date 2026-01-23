package com.game.common.math;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

/**
 * 128位长整数
 * <p>
 * 用于处理超过 Long.MAX_VALUE (2^63-1 = 9223372036854775807) 的大整数场景，
 * 如累计充值总额、全服统计数据、超大计数器等。
 * </p>
 *
 * <pre>
 * 使用示例:
 * {@code
 * Long128 a = Long128.valueOf(Long.MAX_VALUE);
 * Long128 b = Long128.valueOf(1000L);
 * Long128 c = a.add(b);  // 超过 Long.MAX_VALUE
 *
 * // 与 long 比较
 * if (c.compareTo(Long.MAX_VALUE) > 0) {
 *     System.out.println("超过 long 上限");
 * }
 *
 * // 转换为字符串存储
 * String str = c.toString();  // "9223372036854776807"
 * Long128 restored = Long128.parse(str);
 * }
 * </pre>
 *
 * @author GameServer
 */
public final class Long128 implements Comparable<Long128>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 高64位 (有符号)
     */
    private final long high;

    /**
     * 低64位 (无符号处理)
     */
    private final long low;

    // ==================== 常量 ====================

    /**
     * 零
     */
    public static final Long128 ZERO = new Long128(0L, 0L);

    /**
     * 一
     */
    public static final Long128 ONE = new Long128(0L, 1L);

    /**
     * Long 最大值
     */
    public static final Long128 LONG_MAX = new Long128(0L, Long.MAX_VALUE);

    /**
     * Long128 最大值 (有符号: 2^127 - 1)
     */
    public static final Long128 MAX_VALUE = new Long128(Long.MAX_VALUE, -1L);

    /**
     * Long128 最小值 (有符号: -2^127)
     */
    public static final Long128 MIN_VALUE = new Long128(Long.MIN_VALUE, 0L);

    /**
     * 无符号 Long 的最大值 + 1，用于进位计算
     */
    private static final BigInteger UNSIGNED_LONG_MAX_PLUS_ONE = 
            BigInteger.ONE.shiftLeft(64);

    // ==================== 构造器 ====================

    /**
     * 私有构造器
     *
     * @param high 高64位
     * @param low  低64位
     */
    private Long128(long high, long low) {
        this.high = high;
        this.low = low;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 从两个 long 创建
     *
     * @param high 高64位
     * @param low  低64位
     */
    public static Long128 of(long high, long low) {
        if (high == 0 && low == 0) {
            return ZERO;
        }
        if (high == 0 && low == 1) {
            return ONE;
        }
        return new Long128(high, low);
    }

    /**
     * 从 long 创建
     */
    public static Long128 valueOf(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value == 1) {
            return ONE;
        }
        // 负数需要符号扩展
        return new Long128(value < 0 ? -1L : 0L, value);
    }

    /**
     * 从 BigInteger 创建
     */
    public static Long128 valueOf(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) {
            return ZERO;
        }
        if (value.equals(BigInteger.ONE)) {
            return ONE;
        }

        // 检查范围
        if (value.bitLength() > 127) {
            throw new ArithmeticException("Value out of Long128 range: " + value);
        }

        byte[] bytes = value.toByteArray();
        long high = 0;
        long low = 0;

        // 从字节数组解析
        int length = bytes.length;
        for (int i = 0; i < length && i < 8; i++) {
            low |= ((long) bytes[length - 1 - i] & 0xFF) << (i * 8);
        }
        for (int i = 8; i < length && i < 16; i++) {
            high |= ((long) bytes[length - 1 - i] & 0xFF) << ((i - 8) * 8);
        }

        // 处理符号扩展
        if (value.signum() < 0) {
            if (length <= 8) {
                high = -1L;
            }
        }

        return new Long128(high, low);
    }

    /**
     * 从字符串解析
     */
    public static Long128 parse(String value) {
        if (value == null || value.isEmpty()) {
            throw new NumberFormatException("Empty string");
        }
        return valueOf(new BigInteger(value));
    }

    /**
     * 从十六进制字符串解析
     */
    public static Long128 parseHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            throw new NumberFormatException("Empty string");
        }
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        return valueOf(new BigInteger(hex, 16));
    }

    // ==================== 算术运算 ====================

    /**
     * 加法
     */
    public Long128 add(Long128 other) {
        long newLow = this.low + other.low;
        // 检测无符号溢出（进位）
        long carry = Long.compareUnsigned(newLow, this.low) < 0 ? 1 : 0;
        long newHigh = this.high + other.high + carry;
        return Long128.of(newHigh, newLow);
    }

    /**
     * 加法 (long)
     */
    public Long128 add(long value) {
        return add(valueOf(value));
    }

    /**
     * 减法
     */
    public Long128 subtract(Long128 other) {
        long newLow = this.low - other.low;
        // 检测无符号借位
        long borrow = Long.compareUnsigned(this.low, other.low) < 0 ? 1 : 0;
        long newHigh = this.high - other.high - borrow;
        return Long128.of(newHigh, newLow);
    }

    /**
     * 减法 (long)
     */
    public Long128 subtract(long value) {
        return subtract(valueOf(value));
    }

    /**
     * 乘法
     */
    public Long128 multiply(Long128 other) {
        // 使用 BigInteger 进行乘法运算，避免复杂的128位乘法实现
        return valueOf(this.toBigInteger().multiply(other.toBigInteger()));
    }

    /**
     * 乘法 (long)
     */
    public Long128 multiply(long value) {
        return multiply(valueOf(value));
    }

    /**
     * 除法
     */
    public Long128 divide(Long128 other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        return valueOf(this.toBigInteger().divide(other.toBigInteger()));
    }

    /**
     * 除法 (long)
     */
    public Long128 divide(long value) {
        return divide(valueOf(value));
    }

    /**
     * 取模
     */
    public Long128 mod(Long128 other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        return valueOf(this.toBigInteger().mod(other.toBigInteger()));
    }

    /**
     * 取模 (long)
     */
    public Long128 mod(long value) {
        return mod(valueOf(value));
    }

    /**
     * 取反
     */
    public Long128 negate() {
        return ZERO.subtract(this);
    }

    /**
     * 绝对值
     */
    public Long128 abs() {
        return isNegative() ? negate() : this;
    }

    /**
     * 递增
     */
    public Long128 increment() {
        return add(ONE);
    }

    /**
     * 递减
     */
    public Long128 decrement() {
        return subtract(ONE);
    }

    // ==================== 位运算 ====================

    /**
     * 左移
     */
    public Long128 shiftLeft(int n) {
        if (n < 0) {
            return shiftRight(-n);
        }
        if (n == 0) {
            return this;
        }
        if (n >= 128) {
            return ZERO;
        }

        if (n >= 64) {
            return Long128.of(low << (n - 64), 0);
        }

        long newHigh = (high << n) | (low >>> (64 - n));
        long newLow = low << n;
        return Long128.of(newHigh, newLow);
    }

    /**
     * 算术右移 (保留符号)
     */
    public Long128 shiftRight(int n) {
        if (n < 0) {
            return shiftLeft(-n);
        }
        if (n == 0) {
            return this;
        }
        if (n >= 128) {
            return isNegative() ? Long128.of(-1L, -1L) : ZERO;
        }

        if (n >= 64) {
            return Long128.of(high >> 63, high >> (n - 64));
        }

        long newHigh = high >> n;
        long newLow = (low >>> n) | (high << (64 - n));
        return Long128.of(newHigh, newLow);
    }

    /**
     * 按位与
     */
    public Long128 and(Long128 other) {
        return Long128.of(this.high & other.high, this.low & other.low);
    }

    /**
     * 按位或
     */
    public Long128 or(Long128 other) {
        return Long128.of(this.high | other.high, this.low | other.low);
    }

    /**
     * 按位异或
     */
    public Long128 xor(Long128 other) {
        return Long128.of(this.high ^ other.high, this.low ^ other.low);
    }

    /**
     * 按位取反
     */
    public Long128 not() {
        return Long128.of(~high, ~low);
    }

    // ==================== 比较运算 ====================

    @Override
    public int compareTo(Long128 other) {
        // 先比较高位
        if (this.high != other.high) {
            return Long.compare(this.high, other.high);
        }
        // 高位相等，无符号比较低位
        return Long.compareUnsigned(this.low, other.low);
    }

    /**
     * 与 long 比较
     */
    public int compareTo(long value) {
        return compareTo(valueOf(value));
    }

    /**
     * 是否为零
     */
    public boolean isZero() {
        return high == 0 && low == 0;
    }

    /**
     * 是否为负数
     */
    public boolean isNegative() {
        return high < 0;
    }

    /**
     * 是否为正数
     */
    public boolean isPositive() {
        return high > 0 || (high == 0 && low != 0);
    }

    /**
     * 是否可以安全转换为 long
     */
    public boolean fitsInLong() {
        return (high == 0 && low >= 0) || (high == -1 && low < 0);
    }

    /**
     * 是否超过 Long.MAX_VALUE
     */
    public boolean exceedsLongMax() {
        return high > 0 || (high == 0 && low < 0);
    }

    // ==================== 类型转换 ====================

    /**
     * 获取高64位
     */
    public long getHigh() {
        return high;
    }

    /**
     * 获取低64位
     */
    public long getLow() {
        return low;
    }

    /**
     * 转换为 long (可能溢出)
     */
    public long toLong() {
        return low;
    }

    /**
     * 安全转换为 long
     *
     * @throws ArithmeticException 如果超出范围
     */
    public long toLongExact() {
        if (!fitsInLong()) {
            throw new ArithmeticException("Long128 value out of long range: " + this);
        }
        return low;
    }

    /**
     * 转换为 BigInteger
     */
    public BigInteger toBigInteger() {
        if (high == 0 && low >= 0) {
            return BigInteger.valueOf(low);
        }
        if (high == -1 && low < 0) {
            return BigInteger.valueOf(low);
        }

        // 组合高低位
        BigInteger highPart = BigInteger.valueOf(high);
        BigInteger lowPart = BigInteger.valueOf(low);

        // 将低位作为无符号处理
        if (low < 0) {
            lowPart = lowPart.and(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1).or(BigInteger.ONE));
        }

        return highPart.shiftLeft(64).or(lowPart);
    }

    /**
     * 转换为 int (可能溢出)
     */
    public int toInt() {
        return (int) low;
    }

    /**
     * 转换为 double
     */
    public double toDouble() {
        return toBigInteger().doubleValue();
    }

    // ==================== 字符串 ====================

    @Override
    public String toString() {
        return toBigInteger().toString();
    }

    /**
     * 转换为十六进制字符串
     */
    public String toHexString() {
        if (high == 0) {
            return Long.toHexString(low);
        }
        return String.format("%016x%016x", high, low);
    }

    /**
     * 格式化为带分隔符的字符串 (如: 1,234,567,890)
     */
    public String toFormattedString() {
        String str = toString();
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        int start = str.charAt(0) == '-' ? 1 : 0;

        for (int i = start; i < len; i++) {
            if (i > start && (len - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(str.charAt(i));
        }

        return start == 1 ? "-" + sb : sb.toString();
    }

    // ==================== Object 方法 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Long128 long128 = (Long128) o;
        return high == long128.high && low == long128.low;
    }

    @Override
    public int hashCode() {
        return Objects.hash(high, low);
    }

    // ==================== 便捷方法 ====================

    /**
     * 取最大值
     */
    public static Long128 max(Long128 a, Long128 b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * 取最小值
     */
    public static Long128 min(Long128 a, Long128 b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * 求和
     */
    public static Long128 sum(Long128... values) {
        Long128 result = ZERO;
        for (Long128 value : values) {
            result = result.add(value);
        }
        return result;
    }

    /**
     * 从 long 数组求和（用于累加超大数值）
     */
    public static Long128 sumLongs(long... values) {
        Long128 result = ZERO;
        for (long value : values) {
            result = result.add(value);
        }
        return result;
    }
}
