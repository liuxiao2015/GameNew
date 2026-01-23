package com.game.service.rank.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 排行榜类型枚举
 *
 * @author GameServer
 */
public enum RankType {

    /** 战力榜 */
    COMBAT_POWER(1, "combat_power", "战力榜"),
    
    /** 等级榜 */
    LEVEL(2, "level", "等级榜"),
    
    /** 充值榜 */
    RECHARGE(3, "recharge", "充值榜"),
    
    /** 消费榜 */
    CONSUME(4, "consume", "消费榜"),
    
    /** 副本榜 */
    DUNGEON(5, "dungeon", "副本榜"),
    
    /** 竞技场榜 */
    ARENA(6, "arena", "竞技场榜"),
    
    /** 公会榜 */
    GUILD(7, "guild", "公会榜");

    private static final Map<Integer, RankType> TYPE_MAP = new HashMap<>();
    private static final Map<String, RankType> NAME_MAP = new HashMap<>();

    static {
        for (RankType type : values()) {
            TYPE_MAP.put(type.type, type);
            NAME_MAP.put(type.name, type);
        }
    }

    private final int type;
    private final String name;
    private final String desc;

    RankType(int type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    /**
     * 获取类型 ID
     */
    public int getType() {
        return type;
    }

    /**
     * 获取名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取 Redis Key
     */
    public String getRedisKey() {
        return "rank:" + name;
    }

    /**
     * 获取排行榜信息 Redis Key
     */
    public String getInfoKey() {
        return "rank:info:" + name;
    }

    /**
     * 根据类型 ID 获取枚举
     */
    public static RankType of(int type) {
        return TYPE_MAP.get(type);
    }

    /**
     * 根据名称获取枚举
     */
    public static RankType of(String name) {
        return NAME_MAP.get(name);
    }
}
