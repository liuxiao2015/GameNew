package com.game.service.rank.constants;

/**
 * 排行榜类型常量
 *
 * @author GameServer
 */
public final class RankType {

    private RankType() {}

    /**
     * 战力榜
     */
    public static final int COMBAT_POWER = 1;

    /**
     * 等级榜
     */
    public static final int LEVEL = 2;

    /**
     * 充值榜
     */
    public static final int RECHARGE = 3;

    /**
     * 消费榜
     */
    public static final int CONSUME = 4;

    /**
     * 副本榜
     */
    public static final int DUNGEON = 5;

    /**
     * 竞技场榜
     */
    public static final int ARENA = 6;

    /**
     * 公会榜
     */
    public static final int GUILD = 7;

    /**
     * 根据类型 ID 获取名称
     */
    public static String getName(int type) {
        return switch (type) {
            case COMBAT_POWER -> "combat_power";
            case LEVEL -> "level";
            case RECHARGE -> "recharge";
            case CONSUME -> "consume";
            case DUNGEON -> "dungeon";
            case ARENA -> "arena";
            case GUILD -> "guild";
            default -> "unknown";
        };
    }

    /**
     * 根据类型 ID 获取 Redis Key
     */
    public static String getRedisKey(int type) {
        return "rank:" + getName(type);
    }

    /**
     * 根据类型 ID 获取描述
     */
    public static String getDesc(int type) {
        return switch (type) {
            case COMBAT_POWER -> "战力榜";
            case LEVEL -> "等级榜";
            case RECHARGE -> "充值榜";
            case CONSUME -> "消费榜";
            case DUNGEON -> "副本榜";
            case ARENA -> "竞技场榜";
            case GUILD -> "公会榜";
            default -> "未知榜";
        };
    }
}
