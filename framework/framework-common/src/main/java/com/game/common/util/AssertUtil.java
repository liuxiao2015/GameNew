package com.game.common.util;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;

import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 * <p>
 * 用于参数校验，校验失败时抛出 GameException
 * </p>
 *
 * @author GameServer
 */
public final class AssertUtil {

    private AssertUtil() {
        // 禁止实例化
    }

    // ==================== 布尔断言 ====================

    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言条件为真 (自定义消息)
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言条件为假 (自定义消息)
     */
    public static void isFalse(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new GameException(errorCode, message);
        }
    }

    // ==================== 空值断言 ====================

    /**
     * 断言对象不为空
     */
    public static <T> T notNull(T obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new GameException(errorCode);
        }
        return obj;
    }

    /**
     * 断言对象不为空 (自定义消息)
     */
    public static <T> T notNull(T obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new GameException(errorCode, message);
        }
        return obj;
    }

    /**
     * 断言对象为空
     */
    public static void isNull(Object obj, ErrorCode errorCode) {
        if (obj != null) {
            throw new GameException(errorCode);
        }
    }

    // ==================== 字符串断言 ====================

    /**
     * 断言字符串不为空
     */
    public static String notEmpty(String str, ErrorCode errorCode) {
        if (StringUtil.isEmpty(str)) {
            throw new GameException(errorCode);
        }
        return str;
    }

    /**
     * 断言字符串不为空 (自定义消息)
     */
    public static String notEmpty(String str, ErrorCode errorCode, String message) {
        if (StringUtil.isEmpty(str)) {
            throw new GameException(errorCode, message);
        }
        return str;
    }

    /**
     * 断言字符串不为空白
     */
    public static String notBlank(String str, ErrorCode errorCode) {
        if (StringUtil.isBlank(str)) {
            throw new GameException(errorCode);
        }
        return str;
    }

    /**
     * 断言字符串不为空白 (自定义消息)
     */
    public static String notBlank(String str, ErrorCode errorCode, String message) {
        if (StringUtil.isBlank(str)) {
            throw new GameException(errorCode, message);
        }
        return str;
    }

    // ==================== 集合断言 ====================

    /**
     * 断言集合不为空
     */
    public static <T extends Collection<?>> T notEmpty(T collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new GameException(errorCode);
        }
        return collection;
    }

    /**
     * 断言 Map 不为空
     */
    public static <T extends Map<?, ?>> T notEmpty(T map, ErrorCode errorCode) {
        if (map == null || map.isEmpty()) {
            throw new GameException(errorCode);
        }
        return map;
    }

    // ==================== 数值断言 ====================

    /**
     * 断言数值大于 0
     */
    public static long positive(long value, ErrorCode errorCode) {
        if (value <= 0) {
            throw new GameException(errorCode);
        }
        return value;
    }

    /**
     * 断言数值大于等于 0
     */
    public static long notNegative(long value, ErrorCode errorCode) {
        if (value < 0) {
            throw new GameException(errorCode);
        }
        return value;
    }

    /**
     * 断言数值在范围内 (包含边界)
     */
    public static long inRange(long value, long min, long max, ErrorCode errorCode) {
        if (value < min || value > max) {
            throw new GameException(errorCode);
        }
        return value;
    }

    /**
     * 断言数值大于指定值
     */
    public static void greaterThan(long value, long target, ErrorCode errorCode) {
        if (value <= target) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言数值大于等于指定值
     */
    public static void greaterOrEqual(long value, long target, ErrorCode errorCode) {
        if (value < target) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言数值小于指定值
     */
    public static void lessThan(long value, long target, ErrorCode errorCode) {
        if (value >= target) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言数值小于等于指定值
     */
    public static void lessOrEqual(long value, long target, ErrorCode errorCode) {
        if (value > target) {
            throw new GameException(errorCode);
        }
    }

    // ==================== 相等断言 ====================

    /**
     * 断言两个对象相等
     */
    public static void equals(Object obj1, Object obj2, ErrorCode errorCode) {
        if (obj1 == null) {
            if (obj2 != null) {
                throw new GameException(errorCode);
            }
        } else if (!obj1.equals(obj2)) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言两个对象不相等
     */
    public static void notEquals(Object obj1, Object obj2, ErrorCode errorCode) {
        if (obj1 == null) {
            if (obj2 == null) {
                throw new GameException(errorCode);
            }
        } else if (obj1.equals(obj2)) {
            throw new GameException(errorCode);
        }
    }

    // ==================== 状态断言 ====================

    /**
     * 断言状态 (通用状态检查)
     */
    public static void state(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 断言状态 (自定义消息)
     */
    public static void state(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            throw new GameException(errorCode, message);
        }
    }
}
