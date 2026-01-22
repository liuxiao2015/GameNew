package com.game.common.protocol;

import java.lang.annotation.*;

/**
 * Protobuf 字段注解
 * <p>
 * 用于标记 DTO 字段的 Protobuf 属性
 * </p>
 *
 * @author GameServer
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoField {

    /**
     * 字段序号 (Protobuf field number)
     */
    int value();

    /**
     * 字段描述
     */
    String desc() default "";

    /**
     * 是否必填
     */
    boolean required() default false;
}
