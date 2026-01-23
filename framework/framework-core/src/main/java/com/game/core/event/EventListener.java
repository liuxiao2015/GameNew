package com.game.core.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件监听器注解
 * <p>
 * 标记方法为事件处理器，类似 Spring 的 @EventListener
 * </p>
 *
 * <pre>
 * 示例：
 * {@code
 * @Component
 * public class PlayerEventHandler {
 *
 *     @EventListener
 *     public void onPlayerLevelUp(PlayerLevelUpEvent event) {
 *         // 处理玩家升级事件
 *         long roleId = event.getRoleId();
 *         int newLevel = event.getNewLevel();
 *         // 发放升级奖励...
 *     }
 *
 *     @EventListener(async = true)
 *     public void onPlayerLogin(PlayerLoginEvent event) {
 *         // 异步处理登录事件
 *     }
 *
 *     @EventListener(priority = 10)
 *     public void onItemAcquired(ItemAcquiredEvent event) {
 *         // 优先级高的先执行
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

    /**
     * 优先级 (数值越大越先执行)
     */
    int priority() default 0;

    /**
     * 是否异步执行
     */
    boolean async() default false;

    /**
     * 描述
     */
    String desc() default "";
}
