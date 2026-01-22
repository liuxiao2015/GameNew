package com.game.data.mongo.index;

import java.lang.annotation.*;

/**
 * MongoDB 复合索引注解
 * <p>
 * 标记在类上，用于定义复合索引
 * </p>
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CompoundIndexes.class)
public @interface CompoundIndex {

    /**
     * 索引名称
     */
    String name() default "";

    /**
     * 索引定义 (JSON 格式)
     * 例如: {"roleId": 1, "itemId": 1}
     */
    String def();

    /**
     * 是否唯一索引
     */
    boolean unique() default false;

    /**
     * 是否稀疏索引
     */
    boolean sparse() default false;

    /**
     * 是否后台创建
     */
    boolean background() default true;
}
