package com.game.entity.guild;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 公会数据 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "guild")
public class GuildData extends BaseDocument {

    /**
     * 公会 ID
     */
    @MongoIndex(unique = true)
    private long guildId;

    /**
     * 公会名
     */
    @MongoIndex(unique = true)
    private String guildName;

    /**
     * 公会等级
     */
    private int level = 1;

    /**
     * 公会经验
     */
    private long exp = 0;

    /**
     * 公会图标 ID
     */
    private int iconId = 1;

    /**
     * 公会宣言
     */
    private String declaration;

    /**
     * 会长 ID
     */
    @MongoIndex
    private long leaderId;

    /**
     * 会长名字
     */
    private String leaderName;

    /**
     * 成员数量
     */
    private int memberCount = 0;

    /**
     * 最大成员数
     */
    private int maxMember = 30;

    /**
     * 公会创建时间戳
     */
    private long guildCreateTime;

    /**
     * 加入方式 (0:自由 1:审批 2:禁止)
     */
    private int joinType = 0;

    /**
     * 加入等级限制
     */
    private int joinLevel = 0;

    /**
     * 公会资金
     */
    private long fund = 0;

    /**
     * 公会总战力
     */
    private long totalCombatPower = 0;

    /**
     * 上次重置日期
     */
    private String lastResetDate;

    /**
     * 成员列表
     */
    private List<GuildMember> members = new ArrayList<>();

    /**
     * 加入申请列表
     */
    private List<GuildApply> applies = new ArrayList<>();

    // ==================== 公会成员 ====================

    @Data
    public static class GuildMember implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long roleId;
        private String roleName;
        private int level;
        private int avatarId;
        private int position;
        private long contribution = 0;
        private long todayContribution = 0;
        private long weekContribution = 0;
        private long joinTime;
        private long lastLoginTime;
        private long combatPower = 0;
        private boolean online = false;
    }

    // ==================== 加入申请 ====================

    @Data
    public static class GuildApply implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long applyId;
        private long roleId;
        private String roleName;
        private int level;
        private long combatPower;
        private String message;
        private long applyTime;
    }

    // ==================== 静态工厂方法 ====================

    public static GuildData create(long guildId, String guildName, long leaderId,
                                   String leaderName, String declaration, int iconId) {
        GuildData data = new GuildData();
        data.setGuildId(guildId);
        data.setGuildName(guildName);
        data.setLeaderId(leaderId);
        data.setLeaderName(leaderName);
        data.setDeclaration(declaration);
        data.setIconId(iconId);
        data.setGuildCreateTime(System.currentTimeMillis());

        // 添加会长为成员
        GuildMember leader = new GuildMember();
        leader.setRoleId(leaderId);
        leader.setRoleName(leaderName);
        leader.setPosition(3); // 会长
        leader.setJoinTime(System.currentTimeMillis());
        data.getMembers().add(leader);
        data.setMemberCount(1);

        return data;
    }
}
