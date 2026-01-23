package com.game.support.gm.hotfix;

import com.game.core.event.GameEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 热修复脚本同步事件
 * <p>
 * 用于在集群中同步脚本注册/更新/删除操作
 * </p>
 *
 * @author GameServer
 */
@Getter
@NoArgsConstructor
public class HotfixSyncEvent extends GameEvent {

    /**
     * 操作类型
     */
    public enum OperationType {
        REGISTER,   // 注册/更新脚本
        DELETE,     // 删除脚本
        REFRESH     // 刷新所有脚本
    }

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 脚本 ID
     */
    private String scriptId;

    /**
     * 脚本内容 (仅 REGISTER 时有效)
     */
    private String scriptContent;

    /**
     * 是否启用 (仅 REGISTER 时有效)
     */
    private boolean enabled;

    /**
     * 操作者
     */
    private String operator;

    public HotfixSyncEvent(OperationType operationType, String scriptId, String operator) {
        super();
        this.operationType = operationType;
        this.scriptId = scriptId;
        this.operator = operator;
    }

    public HotfixSyncEvent(String scriptId, String scriptContent, boolean enabled, String operator) {
        super();
        this.operationType = OperationType.REGISTER;
        this.scriptId = scriptId;
        this.scriptContent = scriptContent;
        this.enabled = enabled;
        this.operator = operator;
    }

    public static HotfixSyncEvent register(String scriptId, String scriptContent, boolean enabled, String operator) {
        return new HotfixSyncEvent(scriptId, scriptContent, enabled, operator);
    }

    public static HotfixSyncEvent delete(String scriptId, String operator) {
        return new HotfixSyncEvent(OperationType.DELETE, scriptId, operator);
    }

    public static HotfixSyncEvent refresh(String operator) {
        HotfixSyncEvent event = new HotfixSyncEvent();
        event.operationType = OperationType.REFRESH;
        event.operator = operator;
        return event;
    }
}
