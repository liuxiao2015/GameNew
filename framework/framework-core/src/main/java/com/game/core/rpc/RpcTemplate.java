package com.game.core.rpc;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * RPC 调用模板
 * <p>
 * 统一 RPC 调用的异常处理，简化业务代码
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 方式1: 调用并自动处理异常
 * PlayerDTO player = RpcTemplate.call(() -> playerService.getPlayer(roleId));
 *
 * // 方式2: 调用并指定错误码
 * PlayerDTO player = RpcTemplate.call(
 *     () -> playerService.getPlayer(roleId),
 *     ErrorCode.ROLE_NOT_FOUND
 * );
 *
 * // 方式3: 静默调用（失败返回 null，不抛异常）
 * PlayerDTO player = RpcTemplate.callSilent(() -> playerService.getPlayer(roleId));
 *
 * // 方式4: 返回 Result，需要手动检查
 * Result<PlayerDTO> result = RpcTemplate.callForResult(() -> playerService.getPlayer(roleId));
 * if (result.isSuccess()) {
 *     // 处理成功
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public final class RpcTemplate {

    private RpcTemplate() {
    }

    /**
     * 执行 RPC 调用
     * <p>
     * 自动处理异常：
     * - Result 失败 -> 抛出 GameException
     * - 调用异常 -> 抛出 GameException(RPC_ERROR)
     * </p>
     *
     * @param supplier RPC 调用
     * @param <T>      返回数据类型
     * @return 调用结果数据
     */
    public static <T> T call(Supplier<Result<T>> supplier) {
        return call(supplier, ErrorCode.RPC_ERROR);
    }

    /**
     * 执行 RPC 调用 (指定失败错误码)
     *
     * @param supplier RPC 调用
     * @param failCode 失败时的错误码
     * @param <T>      返回数据类型
     * @return 调用结果数据
     */
    public static <T> T call(Supplier<Result<T>> supplier, ErrorCode failCode) {
        try {
            Result<T> result = supplier.get();
            if (result == null) {
                throw new GameException(failCode);
            }
            if (!result.isSuccess()) {
                throw new GameException(result.getCode(), result.getMessage());
            }
            return result.getData();
        } catch (GameException e) {
            throw e;
        } catch (Exception e) {
            log.error("RPC 调用异常", e);
            throw new GameException(failCode, e);
        }
    }

    /**
     * 静默执行 RPC 调用
     * <p>
     * 失败时返回 null，不抛出异常
     * </p>
     *
     * @param supplier RPC 调用
     * @param <T>      返回数据类型
     * @return 调用结果数据，失败返回 null
     */
    public static <T> T callSilent(Supplier<Result<T>> supplier) {
        try {
            Result<T> result = supplier.get();
            if (result != null && result.isSuccess()) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("RPC 静默调用失败", e);
        }
        return null;
    }

    /**
     * 执行 RPC 调用，返回 Result
     * <p>
     * 异常时返回 Result.fail(RPC_ERROR)
     * </p>
     *
     * @param supplier RPC 调用
     * @param <T>      返回数据类型
     * @return Result 对象
     */
    public static <T> Result<T> callForResult(Supplier<Result<T>> supplier) {
        try {
            Result<T> result = supplier.get();
            return result != null ? result : Result.fail(ErrorCode.RPC_ERROR);
        } catch (Exception e) {
            log.error("RPC 调用异常", e);
            return Result.fail(ErrorCode.RPC_ERROR, e.getMessage());
        }
    }

    /**
     * 执行无返回值的 RPC 调用
     *
     * @param supplier RPC 调用
     */
    public static void callVoid(Supplier<Result<Void>> supplier) {
        call(supplier);
    }

    /**
     * 静默执行无返回值的 RPC 调用
     *
     * @param supplier RPC 调用
     */
    public static void callVoidSilent(Supplier<Result<Void>> supplier) {
        callSilent(supplier);
    }

    /**
     * 执行 RPC 调用，结果不为空则校验成功
     *
     * @param supplier RPC 调用
     * @param failCode 结果为空时的错误码
     * @param <T>      返回数据类型
     * @return 调用结果数据 (非空)
     */
    public static <T> T callNotNull(Supplier<Result<T>> supplier, ErrorCode failCode) {
        T data = call(supplier, failCode);
        if (data == null) {
            throw new GameException(failCode);
        }
        return data;
    }
}
