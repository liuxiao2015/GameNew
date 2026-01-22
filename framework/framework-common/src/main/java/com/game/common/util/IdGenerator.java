package com.game.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式 ID 生成器
 * <p>
 * 基于 Snowflake 算法实现，生成 64 位唯一 ID
 * 结构: 1bit 符号位 | 41bit 时间戳 | 10bit 机器ID | 12bit 序列号
 * </p>
 *
 * @author GameServer
 */
public final class IdGenerator {

    /**
     * 起始时间戳 (2024-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 机器 ID 位数
     */
    private static final long WORKER_ID_BITS = 10L;

    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器 ID 最大值 (1023)
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 序列号最大值 (4095)
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器 ID 左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 机器 ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 上次生成 ID 的时间戳
     */
    private volatile long lastTimestamp = -1L;

    /**
     * 默认实例 (机器 ID 为 1)
     */
    private static final IdGenerator DEFAULT_INSTANCE = new IdGenerator(1);

    public IdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("Worker ID 必须在 0-%d 范围内", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }

    /**
     * 获取默认实例
     */
    public static IdGenerator getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * 生成下一个 ID
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        // 时钟回拨处理
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= 5) {
                // 等待时钟追上
                try {
                    Thread.sleep(offset << 1);
                    currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp < lastTimestamp) {
                        throw new RuntimeException("时钟回拨异常，拒绝生成 ID");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待时钟追上时被中断", e);
                }
            } else {
                throw new RuntimeException("时钟回拨过大，拒绝生成 ID");
            }
        }

        // 同一毫秒内，序列号递增
        if (currentTimestamp == lastTimestamp) {
            long seq = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (seq == 0) {
                // 序列号用尽，等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
                sequence.set(0);
            }
        } else {
            // 新的毫秒，序列号重置
            sequence.set(0);
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence.get();
    }

    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 生成玩家 ID
     */
    public static long generatePlayerId() {
        return DEFAULT_INSTANCE.nextId();
    }

    /**
     * 生成公会 ID
     */
    public static long generateGuildId() {
        return DEFAULT_INSTANCE.nextId();
    }

    /**
     * 生成订单 ID
     */
    public static long generateOrderId() {
        return DEFAULT_INSTANCE.nextId();
    }

    /**
     * 生成邮件 ID
     */
    public static long generateMailId() {
        return DEFAULT_INSTANCE.nextId();
    }

    /**
     * 解析 ID 中的时间戳
     */
    public static long parseTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
    }

    /**
     * 解析 ID 中的机器 ID
     */
    public static long parseWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 解析 ID 中的序列号
     */
    public static long parseSequence(long id) {
        return id & MAX_SEQUENCE;
    }
}
