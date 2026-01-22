package com.game.core.handler;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.game.core.context.RequestContext;
import com.game.core.net.session.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议处理器基类
 * <p>
 * 提供通用的工具方法，简化业务开发
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @ProtocolController(moduleId = 0x02, value = "玩家模块")
 * public class PlayerHandler extends BaseHandler {
 *
 *     @ProtocolMapping(methodId = 0x01, desc = "获取玩家信息")
 *     public PlayerInfoResponse getPlayerInfo(GetPlayerInfoRequest request) {
 *         long roleId = getRoleId();
 *         checkRoleId(roleId);
 *
 *         PlayerData player = playerService.getPlayer(roleId);
 *         checkNotNull(player, ErrorCode.ROLE_NOT_FOUND);
 *
 *         return PlayerInfoResponse.newBuilder()
 *             .setRoleId(roleId)
 *             .setNickname(player.getNickname())
 *             .build();
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public abstract class BaseHandler {

    // ==================== 上下文获取 ====================

    /**
     * 获取当前会话
     */
    protected Session getSession() {
        return RequestContext.getSession();
    }

    /**
     * 获取当前角色ID
     */
    protected long getRoleId() {
        return RequestContext.getRoleId();
    }

    /**
     * 获取当前账号ID
     */
    protected long getAccountId() {
        return RequestContext.getAccountId();
    }

    /**
     * 获取当前角色名
     */
    protected String getRoleName() {
        return RequestContext.getRoleName();
    }

    /**
     * 获取当前服务器ID
     */
    protected int getServerId() {
        return RequestContext.getServerId();
    }

    // ==================== 校验方法 ====================

    /**
     * 校验角色ID有效
     */
    protected void checkRoleId(long roleId) {
        BizException.throwIfNotPositive(roleId, ErrorCode.ROLE_NOT_FOUND);
    }

    /**
     * 校验对象不为空
     */
    protected void checkNotNull(Object obj, ErrorCode errorCode) {
        BizException.throwIfNull(obj, errorCode);
    }

    /**
     * 校验对象不为空 (带自定义消息)
     */
    protected void checkNotNull(Object obj, ErrorCode errorCode, String message) {
        BizException.throwIfNull(obj, errorCode, message);
    }

    /**
     * 校验条件为真
     */
    protected void checkTrue(boolean condition, ErrorCode errorCode) {
        BizException.throwIf(!condition, errorCode);
    }

    /**
     * 校验条件为真 (带自定义消息)
     */
    protected void checkTrue(boolean condition, ErrorCode errorCode, String message) {
        BizException.throwIf(!condition, errorCode, message);
    }

    /**
     * 校验条件为假
     */
    protected void checkFalse(boolean condition, ErrorCode errorCode) {
        BizException.throwIf(condition, errorCode);
    }

    /**
     * 校验字符串不为空
     */
    protected void checkNotEmpty(String str, ErrorCode errorCode) {
        BizException.throwIfEmpty(str, errorCode);
    }

    /**
     * 校验数值为正数
     */
    protected void checkPositive(long value, ErrorCode errorCode) {
        BizException.throwIfNotPositive(value, errorCode);
    }

    /**
     * 校验数值范围
     */
    protected void checkRange(long value, long min, long max, ErrorCode errorCode) {
        BizException.throwIf(value < min || value > max, errorCode);
    }

    // ==================== 快捷抛出 ====================

    /**
     * 抛出业务异常
     */
    protected void throwBiz(ErrorCode errorCode) {
        throw new BizException(errorCode);
    }

    /**
     * 抛出业务异常 (带自定义消息)
     */
    protected void throwBiz(ErrorCode errorCode, String message) {
        throw new BizException(errorCode, message);
    }

    // ==================== 日志便捷方法 ====================

    /**
     * 记录调试日志
     */
    protected void logDebug(String message, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug("[roleId={}] " + message, prependRoleId(args));
        }
    }

    /**
     * 记录信息日志
     */
    protected void logInfo(String message, Object... args) {
        log.info("[roleId={}] " + message, prependRoleId(args));
    }

    /**
     * 记录警告日志
     */
    protected void logWarn(String message, Object... args) {
        log.warn("[roleId={}] " + message, prependRoleId(args));
    }

    /**
     * 记录错误日志
     */
    protected void logError(String message, Object... args) {
        log.error("[roleId={}] " + message, prependRoleId(args));
    }

    private Object[] prependRoleId(Object[] args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = getRoleId();
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }
}
