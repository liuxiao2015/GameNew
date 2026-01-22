package com.game.api.guild;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公会成员数据传输对象
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildMemberDTO implements Serializable {

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
     * 职位 (0:成员 1:精英 2:副会长 3:会长)
     */
    private int position;

    /**
     * 贡献值
     */
    private long contribution;

    /**
     * 今日贡献
     */
    private long todayContribution;

    /**
     * 周贡献
     */
    private long weekContribution;

    /**
     * 加入时间
     */
    private long joinTime;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 最后在线时间
     */
    private long lastOnlineTime;

    /**
     * 是否在线
     */
    private boolean online;

    /**
     * 战斗力
     */
    private long combatPower;
}
