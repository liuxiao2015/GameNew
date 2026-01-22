package com.game.service.login.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 角色实体 (基础信息, 用于登录选角)
 *
 * @author GameServer
 */
@Data
@Document(collection = "role")
@CompoundIndexes({
    @CompoundIndex(name = "idx_account_server", def = "{'accountId': 1, 'serverId': 1}")
})
public class Role {

    /**
     * 角色 ID
     */
    @Id
    private long roleId;

    /**
     * 账号 ID
     */
    @Indexed
    private String accountId;

    /**
     * 服务器 ID
     */
    @Indexed
    private int serverId;

    /**
     * 角色名
     */
    @Indexed(unique = true)
    private String roleName;

    /**
     * 等级
     */
    private int level;

    /**
     * 职业
     */
    private int profession;

    /**
     * 头像 ID
     */
    private int avatarId;

    /**
     * 战力
     */
    private long power;

    /**
     * 状态 (0:删除 1:正常)
     */
    private int status = 1;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 更新时间
     */
    private long updateTime;
}
