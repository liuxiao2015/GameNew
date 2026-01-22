package com.game.service.gm.annotation;

import java.lang.annotation.*;

/**
 * GM 操作日志注解
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmLog {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作类型
     */
    String operation();

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResult() default false;
}
