package com.game.common.exception;

import com.game.common.enums.ErrorCode;

import java.io.Serial;

/**
 * 业务异常
 * <p>
 * <strong>已废弃：</strong>请使用 {@link GameException} 代替。
 * 此类保留是为了向后兼容，新代码应使用 GameException。
 * </p>
 *
 * @author GameServer
 * @deprecated 请使用 {@link GameException} 代替
 * @see GameException
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public class BizException extends GameException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BizException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BizException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BizException(int code, String message) {
        super(code, message);
    }

    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
