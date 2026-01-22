package com.game.data.mongo.index;

import java.lang.annotation.*;

/**
 * MongoDB 索引注解
 * <p>
 * 标记在字段上，用于定义 MongoDB 索引
 * </p>
 *
 * @author GameServer
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MongoIndex {

    /**
     * 索引名称 (为空则自动生成)
     */
    String name() default "";

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

    /**
     * 索引顺序 (1: 升序, -1: 降序)
     */
    int order() default 1;

    /**
     * TTL 过期时间 (秒), 0 表示不过期
     */
    long expireAfterSeconds() default 0;
}
