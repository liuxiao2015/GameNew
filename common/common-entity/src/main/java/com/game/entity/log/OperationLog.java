package com.game.entity.log;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.CompoundIndex;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志 MongoDB 文档
 * <p>
 * 记录玩家的各种游戏行为，用于数据分析和问题排查
 * </p>
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "operation_log")
@CompoundIndex(name = "idx_role_type_time", def = "{\"roleId\": 1, \"opType\": 1, \"opTime\": -1}")
@CompoundIndex(name = "idx_type_time", def = "{\"opType\": 1, \"opTime\": -1}")
public class OperationLog extends BaseDocument {

    /**
     * 日志 ID
     */
    @MongoIndex(unique = true)
    private long logId;

    /**
     * 角色 ID
     */
    @MongoIndex
    private long roleId;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 服务器 ID
     */
    @MongoIndex
    private int serverId;

    /**
     * 操作类型
     */
    @MongoIndex
    private String opType;

    /**
     * 操作子类型
     */
    private String opSubType;

    /**
     * 操作时间
     */
    @MongoIndex(expireAfterSeconds = 7776000) // 90 天过期
    private long opTime;

    /**
     * 操作前数据 (JSON)
     */
    private String beforeData;

    /**
     * 操作后数据 (JSON)
     */
    private String afterData;

    /**
     * 变化量
     */
    private long changeAmount;

    /**
     * 关联 ID (物品ID/任务ID等)
     */
    private long relatedId;

    /**
     * 额外数据
     */
    private Map<String, Object> extraData = new HashMap<>();

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 设备 ID
     */
    private String deviceId;
}
