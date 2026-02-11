package com.game.actor.core;

/**
 * Actor 监督策略
 * <p>
 * 当 Actor 处理消息抛出异常时，由监督策略决定如何处理。
 * 借鉴 Akka/Erlang 的 Supervisor 模式，但保持轻量。
 * </p>
 *
 * <pre>
 * 使用方式 1 - Actor 级别覆写:
 * {@code
 * public class PlayerActor extends BaseActor<PlayerData> {
 *     @Override
 *     protected SupervisorStrategy supervisorStrategy() {
 *         return (actor, message, error) -> {
 *             if (error instanceof IllegalStateException) {
 *                 return Directive.RESTART;
 *             }
 *             return Directive.RESUME;
 *         };
 *     }
 * }
 * }
 * </pre>
 *
 * <pre>
 * 使用方式 2 - ActorSystem 全局默认:
 * {@code
 * ActorSystemConfig.create()
 *     .supervisorStrategy(SupervisorStrategy.defaultStrategy());
 * }
 * </pre>
 *
 * @author GameServer
 */
@FunctionalInterface
public interface SupervisorStrategy {

    /**
     * 监督指令
     */
    enum Directive {
        /**
         * 恢复 - 忽略异常，继续处理下一条消息 (默认行为)
         */
        RESUME,

        /**
         * 重启 - 重新加载数据 (调用 loadData())，清空邮箱中的问题消息
         */
        RESTART,

        /**
         * 停止 - 停止该 Actor 并从缓存中移除
         */
        STOP,

        /**
         * 上报 - 将异常传递给 ActorSystem 的全局异常处理器
         */
        ESCALATE
    }

    /**
     * 决定对异常的处理方式
     *
     * @param actor   发生异常的 Actor
     * @param message 导致异常的消息
     * @param error   异常
     * @return 处理指令
     */
    Directive decide(Actor<?> actor, ActorMessage message, Throwable error);

    // ==================== 预定义策略 ====================

    /**
     * 默认策略: 所有异常都恢复 (RESUME), 保持向后兼容
     */
    static SupervisorStrategy defaultStrategy() {
        return (actor, message, error) -> Directive.RESUME;
    }

    /**
     * 严格策略: OOM/StackOverflow 停止, 其他恢复
     */
    static SupervisorStrategy strict() {
        return (actor, message, error) -> {
            if (error instanceof OutOfMemoryError || error instanceof StackOverflowError) {
                return Directive.STOP;
            }
            return Directive.RESUME;
        };
    }

    /**
     * 重启策略: 所有异常都重启 Actor
     */
    static SupervisorStrategy restartOnFailure() {
        return (actor, message, error) -> Directive.RESTART;
    }
}
