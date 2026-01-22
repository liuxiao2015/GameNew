package com.game.entity.chat;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 禁言信息 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "mute_info")
public class MuteInfo extends BaseDocument {

    /**
     * 角色 ID
     */
    @MongoIndex(unique = true)
    private long roleId;

    /**
     * 禁言时间
     */
    private long muteTime;

    /**
     * 禁言结束时间 (0 表示永久)
     */
    @MongoIndex
    private long muteEndTime;

    /**
     * 禁言原因
     */
    private String reason;

    /**
     * 操作人
     */
    private String operator;
}
