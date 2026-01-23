package com.game.core.config.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置容器注解
 * <p>
 * 标记一个类为配置容器，自动加载对应的配置文件
 * </p>
 *
 * <pre>
 * 示例：
 * {@code
 * @Component
 * @ConfigContainer(file = "item.json", configClass = ItemConfig.class)
 * public class ItemConfigContainer extends BaseConfigContainer<ItemConfig> {
 *
 *     // 可以添加自定义查询方法
 *     public List<ItemConfig> getByType(int type) {
 *         return getAll().stream()
 *             .filter(c -> c.getType() == type)
 *             .toList();
 *     }
 *
 *     public List<ItemConfig> getByQuality(int quality) {
 *         return getAll().stream()
 *             .filter(c -> c.getQuality() >= quality)
 *             .toList();
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigContainer {

    /**
     * 配置文件名 (相对于配置目录)
     */
    String file();

    /**
     * 配置类
     */
    Class<? extends GameConfig> configClass();

    /**
     * 是否在启动时加载
     */
    boolean loadOnStartup() default true;

    /**
     * 描述
     */
    String desc() default "";
}
