package com.game.core.service;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.game.core.context.RequestContext;
import com.game.core.event.EventBus;
import com.game.core.event.GameEvent;
import com.game.core.push.PushService;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service 基类
 * <p>
 * 提供通用的服务层工具方法：
 * <ul>
 *     <li>上下文获取</li>
 *     <li>参数校验</li>
 *     <li>事件发布</li>
 *     <li>消息推送</li>
 *     <li>日志记录</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Service
 * public class RewardService extends BaseService {
 *
 *     public void sendReward(long roleId, int rewardId) {
 *         checkPositive(roleId, ErrorCode.PARAM_ERROR);
 *
 *         // 业务逻辑...
 *
 *         // 发布事件
 *         publishEvent(new RewardSentEvent(roleId, rewardId));
 *
 *         // 推送消息
 *         push(roleId, 0x0201, buildRewardMessage());
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public abstract class BaseService {

    @Autowired(required = false)
    protected EventBus eventBus;

    @Autowired(required = false)
    protected PushService pushService;

    // ==================== 上下文获取 ====================

    /**
     * 获取当前角色 ID
     */
    protected long getRoleId() {
        return RequestContext.getRoleId();
    }

    /**
     * 获取当前账号 ID
     */
    protected long getAccountId() {
        return RequestContext.getAccountId();
    }

    /**
     * 获取当前服务器 ID
     */
    protected int getServerId() {
        return RequestContext.getServerId();
    }

    // ==================== 校验方法 ====================

    /**
     * 校验对象不为空
     */
    protected void checkNotNull(Object obj, ErrorCode errorCode) {
        BizException.throwIfNull(obj, errorCode);
    }

    /**
     * 校验对象不为空 (带消息)
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
     * 校验条件为真 (带消息)
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
     * 抛出业务异常 (带消息)
     */
    protected void throwBiz(ErrorCode errorCode, String message) {
        throw new BizException(errorCode, message);
    }

    // ==================== 事件发布 ====================

    /**
     * 发布事件
     */
    protected void publishEvent(GameEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }

    /**
     * 异步发布事件
     */
    protected void publishEventAsync(GameEvent event) {
        if (eventBus != null) {
            eventBus.publishAsync(event);
        }
    }

    // ==================== 消息推送 ====================

    /**
     * 推送消息给玩家
     */
    protected void push(long roleId, int protocolId, Message message) {
        if (pushService != null) {
            pushService.push(roleId, protocolId, message);
        }
    }

    /**
     * 推送消息给玩家 (带模块号和方法号)
     */
    protected void push(long roleId, int moduleId, int methodId, Message message) {
        if (pushService != null) {
            pushService.push(roleId, moduleId, methodId, message);
        }
    }

    /**
     * 广播消息
     */
    protected void broadcast(int protocolId, Message message) {
        if (pushService != null) {
            pushService.broadcast(protocolId, message);
        }
    }

    // ==================== 日志便捷方法 ====================

    /**
     * 记录调试日志 (带 roleId)
     */
    protected void logDebug(String message, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug("[roleId={}] " + message, prependRoleId(args));
        }
    }

    /**
     * 记录信息日志 (带 roleId)
     */
    protected void logInfo(String message, Object... args) {
        log.info("[roleId={}] " + message, prependRoleId(args));
    }

    /**
     * 记录警告日志 (带 roleId)
     */
    protected void logWarn(String message, Object... args) {
        log.warn("[roleId={}] " + message, prependRoleId(args));
    }

    /**
     * 记录错误日志 (带 roleId)
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
