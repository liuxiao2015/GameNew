package com.game.entity.log;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.CompoundIndex;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * GM 操作日志 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "gm_operation_log")
@CompoundIndex(name = "idx_operator_time", def = "{\"operator\": 1, \"opTime\": -1}")
public class GmOperationLog extends BaseDocument {

    /**
     * 日志 ID
     */
    @MongoIndex(unique = true)
    private long logId;

    /**
     * 操作人
     */
    @MongoIndex
    private String operator;

    /**
     * 操作类型
     */
    @MongoIndex
    private String opType;

    /**
     * 操作时间
     */
    @MongoIndex
    private long opTime;

    /**
     * 目标类型 (player/guild/server)
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 操作内容描述
     */
    private String description;

    /**
     * 请求参数 (JSON)
     */
    private String requestParams;

    /**
     * 响应结果 (JSON)
     */
    private String responseResult;

    /**
     * 操作 IP
     */
    private String operatorIp;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
