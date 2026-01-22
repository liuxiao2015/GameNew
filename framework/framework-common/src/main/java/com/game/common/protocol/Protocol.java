package com.game.common.protocol;

import java.lang.annotation.*;

/**
 * 协议处理器注解
 * <p>
 * 用于标记处理特定协议的方法
 * </p>
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Protocol {

    /**
     * 协议号
     */
    int value();

    /**
     * 协议描述
     */
    String desc() default "";

    /**
     * 是否需要登录
     */
    boolean requireLogin() default true;

    /**
     * 是否异步处理
     */
    boolean async() default false;
}
