package com.game.log.gm;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * GM 操作日志
 * <p>
 * 记录 GM 的所有操作，用于审计
 * </p>
 *
 * @author GameServer
 */
@Data
@Document(collection = "gm_log")
@CompoundIndex(name = "idx_gm_time", def = "{'gmAccount': 1, 'logTime': -1}")
@CompoundIndex(name = "idx_target_time", def = "{'targetRoleId': 1, 'logTime': -1}")
public class GmLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    @Id
    private String logId;

    /**
     * GM 账号
     */
    @Indexed
    private String gmAccount;

    /**
     * GM 名称
     */
    private String gmName;

    /**
     * 操作类型
     */
    @Indexed
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * 目标角色 ID
     */
    @Indexed
    private Long targetRoleId;

    /**
     * 目标角色名
     */
    private String targetRoleName;

    /**
     * 请求参数
     */
    private Map<String, Object> requestParams;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 操作结果描述
     */
    private String resultDesc;

    /**
     * 操作 IP
     */
    private String clientIp;

    /**
     * 记录时间
     */
    @Indexed
    private long logTime;

    // ==================== GM 操作类型常量 ====================

    public static final String TYPE_QUERY_PLAYER = "QUERY_PLAYER";
    public static final String TYPE_ADD_GOLD = "ADD_GOLD";
    public static final String TYPE_ADD_DIAMOND = "ADD_DIAMOND";
    public static final String TYPE_ADD_ITEM = "ADD_ITEM";
    public static final String TYPE_SET_LEVEL = "SET_LEVEL";
    public static final String TYPE_BAN_PLAYER = "BAN_PLAYER";
    public static final String TYPE_UNBAN_PLAYER = "UNBAN_PLAYER";
    public static final String TYPE_KICK_PLAYER = "KICK_PLAYER";
    public static final String TYPE_SEND_MAIL = "SEND_MAIL";
    public static final String TYPE_SEND_NOTICE = "SEND_NOTICE";
    public static final String TYPE_RELOAD_CONFIG = "RELOAD_CONFIG";
    public static final String TYPE_SERVER_MAINTENANCE = "SERVER_MAINTENANCE";
}
