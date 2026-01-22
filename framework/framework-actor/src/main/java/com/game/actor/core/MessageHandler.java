package com.game.actor.core;

import java.lang.annotation.*;

/**
 * Actor 消息处理器注解
 * <p>
 * 标记一个方法为 Actor 消息处理方法，框架会自动注册和分发消息
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * public class PlayerActor extends Actor<PlayerData> {
 *
 *     @MessageHandler("LOGIN")
 *     public void onLogin(LoginMessage message) {
 *         // 处理登录消息
 *     }
 *
 *     @MessageHandler("ADD_ITEM")
 *     public AddItemResult onAddItem(AddItemMessage message) {
 *         // 处理添加物品，可以返回结果
 *         return new AddItemResult(true, newCount);
 *     }
 *
 *     @MessageHandler(value = "DAILY_RESET", async = true)
 *     public void onDailyReset() {
 *         // 异步处理每日重置，不阻塞调用方
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {

    /**
     * 消息类型
     */
    String value();

    /**
     * 描述
     */
    String desc() default "";

    /**
     * 是否异步执行（fire-and-forget）
     */
    boolean async() default false;
}
