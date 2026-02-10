package com.game.common.launcher;

import java.lang.annotation.*;

/**
 * 游戏服务标记注解
 * <p>
 * 标记在 Spring Boot 启动类上，使其可被 Launcher 自动发现和启动。
 * 在 IDEA 中运行 LauncherApplication.main() 即可一键启动所有标记了此注解的服务。
 * </p>
 *
 * <pre>
 * {@code
 * @GameService(name = "service-game", order = 30, description = "游戏核心服务")
 * @SpringBootApplication
 * public class GameServiceApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(GameServiceApplication.class, args);
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GameService {

    /**
     * 服务名称 (如 "service-game")
     */
    String name();

    /**
     * 启动顺序 (越小越先启动, 默认 100)
     * <p>
     * 建议分段:
     * <ul>
     *     <li>10: 网关 (Gateway)</li>
     *     <li>20: 登录服务</li>
     *     <li>30-50: 核心业务服务 (Game, Guild, Chat)</li>
     *     <li>60-90: 辅助服务 (Rank, Scheduler, Pay, Activity)</li>
     *     <li>100+: 运营/测试 (GM, Robot, Battle)</li>
     * </ul>
     * </p>
     */
    int order() default 100;

    /**
     * 服务描述 (中文名, 如 "游戏核心服务")
     */
    String description() default "";

    /**
     * 是否默认启用 (设为 false 的服务在一键启动时跳过, 可手动指定启动)
     */
    boolean enabled() default true;
}
