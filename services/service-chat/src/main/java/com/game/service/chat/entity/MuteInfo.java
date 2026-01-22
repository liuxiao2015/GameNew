package com.game.service.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 禁言信息实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "mute_info")
public class MuteInfo {

    /**
     * 角色 ID
     */
    @Id
    private long roleId;

    /**
     * 禁言结束时间 (0 表示永久)
     */
    @Indexed
    private long muteEndTime;

    /**
     * 禁言原因
     */
    private String reason;

    /**
     * 禁言时间
     */
    private long muteTime;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 检查是否被禁言
     */
    public boolean isMuted() {
        if (muteEndTime == 0) {
            return true; // 永久禁言
        }
        return System.currentTimeMillis() < muteEndTime;
    }
}
