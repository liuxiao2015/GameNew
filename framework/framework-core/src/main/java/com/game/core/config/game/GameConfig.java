package com.game.core.config.game;

/**
 * 游戏配置接口
 * <p>
 * 所有游戏配置类需要实现此接口
 * </p>
 *
 * <pre>
 * 示例：
 * {@code
 * @Data
 * public class ItemConfig implements GameConfig {
 *     private int id;
 *     private String name;
 *     private int type;
 *     private int quality;
 *     private int stackLimit;
 *     private int sellPrice;
 *
 *     @Override
 *     public int getId() {
 *         return id;
 *     }
 *
 *     @Override
 *     public void afterLoad() {
 *         // 加载后的校验或预处理
 *         if (stackLimit <= 0) stackLimit = 1;
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
public interface GameConfig {

    /**
     * 获取配置 ID
     */
    int getId();

    /**
     * 配置加载后的回调 (用于校验、预处理)
     */
    default void afterLoad() {
        // 默认空实现
    }

    /**
     * 配置校验
     *
     * @return 校验错误信息，null 或空表示校验通过
     */
    default String validate() {
        return null;
    }
}
