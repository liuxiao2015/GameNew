package com.game.log.operation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 操作日志
 * <p>
 * 记录玩家的关键操作，用于数据分析和问题追溯
 * </p>
 *
 * @author GameServer
 */
@Data
@Document(collection = "operation_log")
@CompoundIndex(name = "idx_role_time", def = "{'roleId': 1, 'logTime': -1}")
@CompoundIndex(name = "idx_type_time", def = "{'logType': 1, 'logTime': -1}")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    @Id
    private String logId;

    /**
     * 角色 ID
     */
    @Indexed
    private long roleId;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 日志类型
     */
    @Indexed
    private String logType;

    /**
     * 日志子类型
     */
    private String subType;

    /**
     * 变化前的值
     */
    private Object beforeValue;

    /**
     * 变化后的值
     */
    private Object afterValue;

    /**
     * 变化量
     */
    private Long changeAmount;

    /**
     * 来源/原因
     */
    private String reason;

    /**
     * 扩展数据
     */
    private Map<String, Object> extra;

    /**
     * 记录时间
     */
    @Indexed
    private long logTime;

    /**
     * 服务器 ID
     */
    private int serverId;

    /**
     * 客户端 IP
     */
    private String clientIp;

    // ==================== 日志类型常量 ====================

    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_LOGOUT = "LOGOUT";
    public static final String TYPE_CREATE_ROLE = "CREATE_ROLE";
    public static final String TYPE_LEVEL_UP = "LEVEL_UP";
    public static final String TYPE_GOLD_CHANGE = "GOLD_CHANGE";
    public static final String TYPE_DIAMOND_CHANGE = "DIAMOND_CHANGE";
    public static final String TYPE_ITEM_CHANGE = "ITEM_CHANGE";
    public static final String TYPE_EQUIPMENT_CHANGE = "EQUIPMENT_CHANGE";
    public static final String TYPE_GUILD_JOIN = "GUILD_JOIN";
    public static final String TYPE_GUILD_LEAVE = "GUILD_LEAVE";
    public static final String TYPE_QUEST_COMPLETE = "QUEST_COMPLETE";
    public static final String TYPE_RECHARGE = "RECHARGE";
    public static final String TYPE_CONSUME = "CONSUME";
    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_GM_OPERATION = "GM_OPERATION";
}
