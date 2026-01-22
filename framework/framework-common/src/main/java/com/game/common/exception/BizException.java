package com.game.common.exception;

import com.game.common.enums.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 * <p>
 * 开发者可在任何地方抛出此异常，框架会自动捕获并转换为客户端响应
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 方式1: 使用 ErrorCode
 * throw new BizException(ErrorCode.PARAM_ERROR);
 *
 * // 方式2: 自定义消息
 * throw new BizException(ErrorCode.PARAM_ERROR, "用户名不能为空");
 *
 * // 方式3: 快捷抛出
 * BizException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
 * BizException.throwIfNull(user, ErrorCode.USER_NOT_FOUND);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误码枚举 (可选)
     */
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = ErrorCode.fromCode(code);
    }

    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    // ==================== 快捷方法 ====================

    /**
     * 条件为 true 时抛出异常
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 条件为 true 时抛出异常 (带自定义消息)
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 对象为 null 时抛出异常
     */
    public static void throwIfNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 对象为 null 时抛出异常 (带自定义消息)
     */
    public static void throwIfNull(Object obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 字符串为空时抛出异常
     */
    public static void throwIfEmpty(String str, ErrorCode errorCode) {
        if (str == null || str.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 字符串为空时抛出异常 (带自定义消息)
     */
    public static void throwIfEmpty(String str, ErrorCode errorCode, String message) {
        if (str == null || str.isEmpty()) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 数值小于等于0时抛出异常
     */
    public static void throwIfNotPositive(long value, ErrorCode errorCode) {
        if (value <= 0) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 数值小于等于0时抛出异常 (带自定义消息)
     */
    public static void throwIfNotPositive(long value, ErrorCode errorCode, String message) {
        if (value <= 0) {
            throw new BizException(errorCode, message);
        }
    }

    @Override
    public String toString() {
        return "BizException{code=" + code + ", message='" + getMessage() + "'}";
    }
}
