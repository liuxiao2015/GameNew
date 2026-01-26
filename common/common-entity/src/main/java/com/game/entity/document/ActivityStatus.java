package com.game.entity.document;

import lombok.Getter;

/**
 * 活动状态
 *
 * @author GameServer
 */
@Getter
public enum ActivityStatus {

    NOT_STARTED(0, "未开始"),
    PREVIEW(1, "预告中"),
    RUNNING(2, "进行中"),
    ENDED(3, "已结束"),
    CLOSED(4, "已关闭"),
    DISABLED(5, "已禁用");

    private final int code;
    private final String name;

    ActivityStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ActivityStatus of(int code) {
        for (ActivityStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean isVisible() {
        return this == PREVIEW || this == RUNNING || this == ENDED;
    }

    public boolean canParticipate() {
        return this == RUNNING;
    }

    public boolean canClaimReward() {
        return this == RUNNING || this == ENDED;
    }
}
