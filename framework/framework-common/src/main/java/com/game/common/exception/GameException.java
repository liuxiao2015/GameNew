package com.game.common.exception;

import com.game.common.enums.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 游戏业务异常
 * <p>
 * 用于业务逻辑中的异常抛出，会被全局异常处理器捕获并转换为统一响应
 * </p>
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

    /**
     * 获取错误码数值
     */
    public int getCode() {
        return errorCode.getCode();
    }

    /**
     * 静态工厂方法
     */
    public static GameException of(ErrorCode errorCode) {
        return new GameException(errorCode);
    }

    public static GameException of(ErrorCode errorCode, String message) {
        return new GameException(errorCode, message);
    }

    public static GameException of(ErrorCode errorCode, Object data) {
        return new GameException(errorCode, data);
    }

    /**
     * 断言，条件为 false 时抛出异常
     */
    public static void assertTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new GameException(errorCode);
        }
    }

    public static void assertTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new GameException(errorCode, message);
        }
    }

    /**
     * 断言非空
     */
    public static <T> T assertNotNull(T obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new GameException(errorCode);
        }
        return obj;
    }

    public static <T> T assertNotNull(T obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new GameException(errorCode, message);
        }
        return obj;
    }
}
