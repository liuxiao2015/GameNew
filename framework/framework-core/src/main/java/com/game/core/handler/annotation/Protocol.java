package com.game.core.handler.annotation;

import java.lang.annotation.*;

/**
 * 统一协议处理器注解
 * <p>
 * 用于标记处理特定协议的方法，适用于网关和后端服务。
 * 支持两种协议号定义方式：
 * <ol>
 *     <li>直接指定完整协议号: value = 1001</li>
 *     <li>模块号 + 方法号: 配合 @ProtocolController 使用</li>
 * </ol>
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * @ProtocolController(moduleId = 0x1000, value = "登录模块")
 * public class LoginHandler {
 *     
 *     @Protocol(methodId = 0x01, desc = "登录")
 *     public LoginResponse login(Session session, LoginRequest request) {
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author GameServer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Protocol {

    /**
     * 完整协议号 (优先级最高)
     * <p>
     * 如果指定了此值，则忽略 methodId
     * </p>
     */
    int value() default 0;

    /**
     * 方法号 (与 @ProtocolController 的 moduleId 组合)
     */
    int methodId() default 0;

    /**
     * 协议描述 (用于日志和文档)
     */
    String desc() default "";

    /**
     * 是否需要登录验证
     */
    boolean requireLogin() default true;

    /**
     * 是否需要角色验证
     */
    boolean requireRole() default true;

    /**
     * 限流配置 (每秒请求数)，0 表示不限流
     */
    int rateLimit() default 0;

    /**
     * 慢请求阈值 (毫秒)，超过此值会记录警告日志
     */
    int slowThreshold() default 100;

    /**
     * 是否异步执行 (在单独的虚拟线程中执行)
     */
    boolean async() default false;

    /**
     * 是否在 Actor 中执行
     */
    boolean executeInActor() default false;
}
