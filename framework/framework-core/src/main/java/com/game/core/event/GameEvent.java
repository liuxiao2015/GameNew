package com.game.core.event;

/**
 * 游戏事件接口
 * <p>
 * 所有游戏内事件都实现此接口。支持两种实现方式：
 * <ul>
 *     <li>record 实现（简单不可变事件）</li>
 *     <li>class 实现（需要可取消等高级功能）</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 示例 1 - Record 实现：
 * {@code
 * public record PlayerLevelUpEvent(long roleId, int oldLevel, int newLevel) implements GameEvent {
 *     @Override
 *     public String getEventType() {
 *         return "PlayerLevelUp";
 *     }
 * }
 * }
 * </pre>
 *
 * <pre>
 * 示例 2 - Class 实现：
 * {@code
 * public class PlayerLevelUpEvent extends BaseGameEvent {
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
public interface GameEvent {

    /**
     * 获取事件类型名
     */
    String getEventType();

    /**
     * 获取事件时间戳
     */
    default long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取关联的角色ID
     */
    default long getRoleId() {
        return 0L;
    }

    /**
     * 是否已取消
     */
    default boolean isCancelled() {
        return false;
    }
}
