package com.game.entity.document;

import lombok.Getter;

/**
 * 活动类型
 *
 * @author GameServer
 */
@Getter
public enum ActivityType {

    DAILY(1, "日常活动"),
    PERIODIC(2, "周期活动"),
    LIMITED(3, "限时活动"),
    PERMANENT(4, "永久活动"),
    OPEN_SERVER(5, "开服活动"),
    MERGE_SERVER(6, "合服活动"),
    FESTIVAL(7, "节日活动"),
    OPERATION(8, "运营活动");

    private final int code;
    private final String name;

    ActivityType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ActivityType of(int code) {
        for (ActivityType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
