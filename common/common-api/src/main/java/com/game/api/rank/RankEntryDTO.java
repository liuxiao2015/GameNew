package com.game.api.rank;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 排行条目 DTO
 *
 * @author GameServer
 */
@Data
public class RankEntryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 名次
     */
    private int rank;

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
     * 公会 ID
     */
    private long guildId;

    /**
     * 公会名
     */
    private String guildName;

    /**
     * 分数
     */
    private long score;

    /**
     * 扩展信息
     */
    private Map<String, String> extra;
}
