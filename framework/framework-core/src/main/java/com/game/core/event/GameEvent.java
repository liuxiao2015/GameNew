package com.game.core.event;

import lombok.Getter;

/**
 * 游戏事件基类
 * <p>
 * 所有游戏内事件都继承此类
 * </p>
 *
 * <pre>
 * 示例：
 * {@code
 * public class PlayerLevelUpEvent extends GameEvent {
 *     private final int oldLevel;
 *     private final int newLevel;
 *
 *     public PlayerLevelUpEvent(long roleId, int oldLevel, int newLevel) {
 *         super(roleId);
 *         this.oldLevel = oldLevel;
 *         this.newLevel = newLevel;
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Getter
public abstract class GameEvent {

    /**
     * 事件发生时间
     */
    private final long timestamp;

    /**
     * 关联的角色ID (0 表示系统事件)
     */
    private final long roleId;

    /**
     * 是否已取消
     */
    private boolean cancelled;

    protected GameEvent() {
        this(0);
    }

    protected GameEvent(long roleId) {
        this.roleId = roleId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 取消事件 (阻止后续处理)
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 获取事件类型名
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
