package com.game.core.cross;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 跨服消息
 * <p>
 * 用于服务器之间的消息传递
 * </p>
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 来源服务器 ID
     */
    private int sourceServerId;

    /**
     * 目标服务器 ID (0 表示广播)
     */
    private int targetServerId;

    /**
     * 来源角色 ID
     */
    private long sourceRoleId;

    /**
     * 目标角色 ID
     */
    private long targetRoleId;

    /**
     * 消息内容 (JSON 格式)
     */
    private String payload;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 消息 ID (用于去重)
     */
    private String messageId;

    // ==================== 预定义消息类型 ====================

    /** 跨服聊天 */
    public static final String TYPE_CHAT = "CROSS_CHAT";
    /** 跨服邮件 */
    public static final String TYPE_MAIL = "CROSS_MAIL";
    /** 跨服公会 */
    public static final String TYPE_GUILD = "CROSS_GUILD";
    /** 跨服组队 */
    public static final String TYPE_TEAM = "CROSS_TEAM";
    /** 跨服战斗 */
    public static final String TYPE_BATTLE = "CROSS_BATTLE";
    /** 服务器广播 */
    public static final String TYPE_BROADCAST = "CROSS_BROADCAST";

    /**
     * 创建跨服消息
     */
    public static CrossMessage create(String type, int sourceServerId, int targetServerId,
                                       long sourceRoleId, long targetRoleId, String payload) {
        return CrossMessage.builder()
                .type(type)
                .sourceServerId(sourceServerId)
                .targetServerId(targetServerId)
                .sourceRoleId(sourceRoleId)
                .targetRoleId(targetRoleId)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .messageId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * 创建广播消息
     */
    public static CrossMessage broadcast(String type, int sourceServerId, String payload) {
        return create(type, sourceServerId, 0, 0, 0, payload);
    }
}
