package com.game.common.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.lang.Snowflake;

/**
 * 分布式 ID 生成器 (基于 Hutool Snowflake)
 * <p>
 * 保持原有 API 不变，内部委托 Hutool {@link Snowflake}。
 * 生成 64 位唯一 ID，结构: 1bit 符号位 | 41bit 时间戳 | 5bit 数据中心 | 5bit 机器ID | 12bit 序列号
 * </p>
 *
 * @author GameServer
 */
public final class IdGenerator {

    /**
     * 底层 Hutool Snowflake 实例
     */
    private final Snowflake snowflake;

    /**
     * 默认实例 (workerId=1, dataCenterId=0)
     */
    private static final IdGenerator DEFAULT_INSTANCE = new IdGenerator(1);

    public IdGenerator(long workerId) {
        // dataCenterId 默认为 0，workerId 映射到 Hutool 的 workerId
        this.snowflake = IdUtil.getSnowflake(workerId, 0);
    }

    public IdGenerator(long workerId, long dataCenterId) {
        this.snowflake = IdUtil.getSnowflake(workerId, dataCenterId);
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
    public long nextId() {
        return snowflake.nextId();
    }

    /**
     * 生成下一个 ID (字符串)
     */
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }

    // ==================== 静态便捷方法 ====================

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
        // Hutool Snowflake: 默认开始时间为 2010-11-04 09:42:54
        // (id >> 22) + twepoch
        return (id >> 22) + 1288834974657L;
    }

    /**
     * 解析 ID 中的机器 ID
     */
    public static long parseWorkerId(long id) {
        return (id >> 12) & 0x1F; // 5 bits
    }

    /**
     * 解析 ID 中的序列号
     */
    public static long parseSequence(long id) {
        return id & 0xFFF; // 12 bits
    }
}
