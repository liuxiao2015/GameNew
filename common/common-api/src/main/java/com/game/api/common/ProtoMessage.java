package com.game.api.common;

import java.lang.annotation.*;

/**
 * Protobuf 消息注解
 * <p>
 * 用于标记需要生成 .proto 文件的 DTO 类
 * </p>
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoMessage {

    /**
     * 消息名称 (为空则使用类名)
     */
    String name() default "";

    /**
     * 消息描述
     */
    String desc() default "";

    /**
     * 所属模块
     */
    String module() default "common";

    /**
     * 协议号 (用于请求/响应消息)
     */
    int protocolId() default 0;
}
