package com.game.api.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色简要信息数据传输对象
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleBriefDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    private long roleId;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 等级
     */
    private int level;

    /**
     * 头像 ID
     */
    private int avatarId;

    /**
     * 头像框 ID
     */
    private int frameId;

    /**
     * VIP 等级
     */
    private int vipLevel;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 所在服务器 ID
     */
    private int serverId;

    /**
     * 创建时间
     */
    private long createTime;
}
