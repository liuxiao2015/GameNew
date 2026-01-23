package com.game.core.event;

import lombok.Getter;

/**
 * 游戏事件基类
 * <p>
 * 提供事件的基本实现，支持取消等高级功能。
 * 对于简单的不可变事件，推荐使用 record 直接实现 GameEvent 接口。
 * </p>
 *
 * @author GameServer
 */
@Getter
public abstract class BaseGameEvent implements GameEvent {

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

    protected BaseGameEvent() {
        this(0);
    }

    protected BaseGameEvent(long roleId) {
        this.roleId = roleId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 取消事件 (阻止后续处理)
     */
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
