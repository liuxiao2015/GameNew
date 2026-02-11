package com.game.actor.core;

/**
 * 邮箱溢出策略 (背压机制)
 * <p>
 * 当 Actor 的消息队列满时，决定如何处理新到来的消息。
 * </p>
 *
 * @author GameServer
 */
public enum MailboxOverflowStrategy {

    /**
     * 丢弃新消息 (默认, 向后兼容)
     * <p>
     * 邮箱满时 tell() 返回 false，新消息被丢弃。
     * 适用于: 非关键消息、可丢失的推送。
     * </p>
     */
    DROP_NEW,

    /**
     * 丢弃最旧消息
     * <p>
     * 邮箱满时移除队列头部最旧的消息，为新消息腾出空间。
     * 适用于: 实时性要求高，旧消息已过期的场景 (如位置同步)。
     * </p>
     */
    DROP_OLDEST,

    /**
     * 阻塞发送方 (带超时)
     * <p>
     * 邮箱满时阻塞 tell() 调用最多 3 秒，等待消费。
     * 超时后返回 false。
     * 适用于: 重要消息不能丢失，但可接受短暂延迟。
     * </p>
     */
    BLOCK_WITH_TIMEOUT,

    /**
     * 动态扩容
     * <p>
     * 邮箱满时自动扩容为无界队列，但超过软上限后打印告警日志。
     * 适用于: 突发流量场景，宁可内存增长也不丢消息。
     * </p>
     */
    DYNAMIC_GROW
}
