package com.game.entity.document;

import lombok.Getter;

/**
 * 活动重置类型
 *
 * @author GameServer
 */
@Getter
public enum ResetType {

    NONE(0, "不重置"),
    DAILY(1, "每日重置"),
    WEEKLY(2, "每周重置"),
    MONTHLY(3, "每月重置"),
    ACTIVITY_CYCLE(4, "活动周期重置");

    private final int code;
    private final String name;

    ResetType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ResetType of(int code) {
        for (ResetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return NONE;
    }
}
