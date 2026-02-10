package com.game.entity.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * GM 操作日志实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "gm_operation_log")
@CompoundIndexes({
    @CompoundIndex(name = "idx_operator_time", def = "{'operator': 1, 'operateTime': -1}"),
    @CompoundIndex(name = "idx_type_time", def = "{'operationType': 1, 'operateTime': -1}")
})
public class GmOperationLog {

    /**
     * 日志 ID
     */
    @Id
    private String id;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseResult;

    /**
     * 目标角色 ID (如果有)
     */
    private long targetRoleId;

    /**
     * 操作 IP
     */
    private String operateIp;

    /**
     * 操作时间
     */
    private long operateTime;

    /**
     * 操作状态 (0:失败 1:成功)
     */
    private int status;

    /**
     * 备注
     */
    private String remark;
}
