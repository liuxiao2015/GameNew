package com.game.common.exception;

import com.game.common.enums.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 游戏业务异常
 * <p>
 * 用于业务逻辑中的异常抛出，会被全局异常处理器捕获并转换为统一响应。
 * 这是项目的统一业务异常类，推荐使用此类而非 BizException。
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 方式1: 使用 ErrorCode
 * throw new GameException(ErrorCode.PARAM_ERROR);
 *
 * // 方式2: 自定义消息
 * throw new GameException(ErrorCode.PARAM_ERROR, "用户名不能为空");
 *
 * // 方式3: 快捷抛出
 * GameException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
 * GameException.throwIfNull(user, ErrorCode.USER_NOT_FOUND);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Getter
public class GameException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * 扩展数据 (可选)
     */
    private final Object data;

    public GameException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public GameException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public GameException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }

    public GameException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public GameException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.data = null;
    }

    public GameException(int code, String message) {
        super(message);
        this.errorCode = ErrorCode.fromCode(code);
        this.data = null;
    }

    /**
     * 获取错误码数值
     */
    public int getCode() {
        return errorCode != null ? errorCode.getCode() : -1;
    }

    // ==================== 静态工厂方法 ====================

    public static GameException of(ErrorCode errorCode) {
        return new GameException(errorCode);
    }

    public static GameException of(ErrorCode errorCode, String message) {
        return new GameException(errorCode, message);
    }

    public static GameException of(ErrorCode errorCode, Object data) {
        return new GameException(errorCode, data);
    }

    // ==================== 快捷断言方法 ====================

    /**
     * 条件为 true 时抛出异常
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 条件为 true 时抛出异常 (带自定义消息)
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 条件为 false 时抛出异常
     */
    public static void assertTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 条件为 false 时抛出异常 (带自定义消息)
     */
    public static void assertTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 对象为 null 时抛出异常
     */
    public static void throwIfNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 对象为 null 时抛出异常 (带自定义消息)
     */
    public static void throwIfNull(Object obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 断言非空并返回
     */
    public static <T> T assertNotNull(T obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new GameException(errorCode);
        }
        return obj;
    }

    /**
     * 断言非空并返回 (带自定义消息)
     */
    public static <T> T assertNotNull(T obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new GameException(errorCode, message);
        }
        return obj;
    }

    /**
     * 字符串为空时抛出异常
     */
    public static void throwIfEmpty(String str, ErrorCode errorCode) {
        if (str == null || str.isEmpty()) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 字符串为空时抛出异常 (带自定义消息)
     */
    public static void throwIfEmpty(String str, ErrorCode errorCode, String message) {
        if (str == null || str.isEmpty()) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 数值小于等于0时抛出异常
     */
    public static void throwIfNotPositive(long value, ErrorCode errorCode) {
        if (value <= 0) {
            throw new GameException(errorCode);
        }
    }

    /**
     * 数值小于等于0时抛出异常 (带自定义消息)
     */
    public static void throwIfNotPositive(long value, ErrorCode errorCode, String message) {
        if (value <= 0) {
            throw new GameException(errorCode, message);
        }
    }

    @Override
    public String toString() {
        return "GameException{code=" + getCode() + ", message='" + getMessage() + "'}";
    }
}
