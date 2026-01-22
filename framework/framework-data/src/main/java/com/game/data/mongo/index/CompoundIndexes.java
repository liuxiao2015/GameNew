package com.game.data.mongo.index;

import java.lang.annotation.*;

/**
 * MongoDB 复合索引容器注解
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CompoundIndexes {

    CompoundIndex[] value();
}
