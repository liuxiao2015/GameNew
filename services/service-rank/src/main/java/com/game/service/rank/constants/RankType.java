package com.game.service.rank.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 排行榜类型
 *
 * @author GameServer
 */
@Getter
@RequiredArgsConstructor
public enum RankType {

    /**
     * 战力榜
     */
    POWER(1, "combat-power", "rank:power", "战力榜"),

    /**
     * 等级榜
     */
    LEVEL(2, "level", "rank:level", "等级榜"),

    /**
     * 充值榜
     */
    RECHARGE(3, "recharge", "rank:recharge", "充值榜"),

    /**
     * 消费榜
     */
    CONSUME(4, "consume", "rank:consume", "消费榜"),

    /**
     * 副本榜
     */
    DUNGEON(5, "dungeon", "rank:dungeon", "副本榜"),

    /**
     * 竞技场榜
     */
    ARENA(6, "arena", "rank:arena", "竞技场榜"),

    /**
     * 公会榜
     */
    GUILD(7, "guild", "rank:guild", "公会榜"),

    ;

    /**
     * 类型 ID
     */
    private final int type;

    /**
     * 类型名称 (字符串标识)
     */
    private final String name;

    /**
     * Redis Key
     */
    private final String redisKey;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 根据类型 ID 获取枚举
     */
    public static RankType of(int type) {
        for (RankType rankType : values()) {
            if (rankType.type == type) {
                return rankType;
            }
        }
        return null;
    }

    /**
     * 根据类型名称获取枚举
     */
    public static RankType of(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (RankType rankType : values()) {
            if (rankType.name.equalsIgnoreCase(name)) {
                return rankType;
            }
        }
        return null;
    }

    /**
     * 获取玩家信息缓存 Key
     */
    public String getInfoKey() {
        return redisKey + ":info";
    }
}
