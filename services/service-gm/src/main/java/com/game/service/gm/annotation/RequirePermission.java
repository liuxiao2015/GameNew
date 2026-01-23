package com.game.service.gm.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解
 *
 * @author GameServer
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 所需权限
     */
    String value();

    /**
     * 逻辑关系 (AND/OR)
     */
    String logic() default "AND";
}
