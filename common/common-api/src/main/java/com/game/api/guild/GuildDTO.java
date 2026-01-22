package com.game.api.guild;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公会数据传输对象
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公会 ID
     */
    private long guildId;

    /**
     * 公会名
     */
    private String guildName;

    /**
     * 公会等级
     */
    private int level;

    /**
     * 公会经验
     */
    private long exp;

    /**
     * 公会图标 ID
     */
    private int iconId;

    /**
     * 公会宣言
     */
    private String declaration;

    /**
     * 会长 ID
     */
    private long leaderId;

    /**
     * 会长名字
     */
    private String leaderName;

    /**
     * 成员数量
     */
    private int memberCount;

    /**
     * 最大成员数
     */
    private int maxMember;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 加入方式 (0:自由 1:审批 2:禁止)
     */
    private int joinType;

    /**
     * 加入等级限制
     */
    private int joinLevel;

    /**
     * 公会资金
     */
    private long fund;

    /**
     * 公会总战力
     */
    private long totalPower;

    /**
     * 公会成员列表
     */
    private java.util.List<GuildMemberDTO> members;
}
