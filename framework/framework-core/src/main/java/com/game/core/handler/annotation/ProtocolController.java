package com.game.core.handler.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 协议控制器注解
 * <p>
 * 用于标记处理客户端请求的类，类似 Spring MVC 的 @Controller。
 * 配合 @Protocol 注解使用，实现基于模块的协议分组。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @ProtocolController(moduleId = 0x1000, value = "登录模块")
 * public class LoginHandler extends BaseHandler {
 *     
 *     @Protocol(methodId = 0x01, desc = "登录")
 *     public LoginResponse login(Session session, LoginRequest request) {
 *         // 业务逻辑
 *     }
 * }
 * }</pre>
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ProtocolController {

    /**
     * 模块 ID
     * <p>
     * 与 @Protocol 的 methodId 组合生成完整协议号:
     * protocolKey = (moduleId << 8) | methodId
     * </p>
     */
    int moduleId() default 0;

    /**
     * 模块描述
     */
    String value() default "";
}
