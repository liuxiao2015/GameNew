package com.game.api.player;

import com.game.common.protocol.ProtoField;
import com.game.common.protocol.ProtoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 玩家数据传输对象
 * <p>
 * 包含 Protobuf 字段注解，可用于自动生成 .proto 文件
 * </p>
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ProtoMessage(module = "player", desc = "玩家基础信息")
public class PlayerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    @ProtoField(value = 1, desc = "角色ID")
    private long roleId;

    /**
     * 角色名
     */
    @ProtoField(value = 2, desc = "角色名")
    private String roleName;

    /**
     * 等级
     */
    @ProtoField(value = 3, desc = "等级")
    private int level;

    /**
     * 经验
     */
    @ProtoField(value = 4, desc = "经验")
    private long exp;

    /**
     * VIP 等级
     */
    @ProtoField(value = 5, desc = "VIP等级")
    private int vipLevel;

    /**
     * 头像 ID
     */
    @ProtoField(value = 6, desc = "头像ID")
    private int avatarId;

    /**
     * 头像框 ID
     */
    @ProtoField(value = 7, desc = "头像框ID")
    private int frameId;

    /**
     * 金币
     */
    @ProtoField(value = 8, desc = "金币")
    private long gold;

    /**
     * 钻石
     */
    @ProtoField(value = 9, desc = "钻石")
    private long diamond;

    /**
     * 绑定钻石
     */
    @ProtoField(value = 10, desc = "绑定钻石")
    private long bindDiamond;

    /**
     * 战斗力
     */
    @ProtoField(value = 11, desc = "战斗力")
    private long combatPower;

    /**
     * 体力
     */
    @ProtoField(value = 12, desc = "体力")
    private int energy;

    /**
     * 公会 ID
     */
    @ProtoField(value = 13, desc = "公会ID")
    private long guildId;

    /**
     * 公会名称
     */
    @ProtoField(value = 14, desc = "公会名称")
    private String guildName;

    /**
     * 公会职位
     */
    @ProtoField(value = 15, desc = "公会职位")
    private int guildPosition;

    /**
     * 服务器 ID
     */
    @ProtoField(value = 16, desc = "服务器ID")
    private int serverId;

    /**
     * 最后登录时间
     */
    @ProtoField(value = 17, desc = "最后登录时间戳")
    private long lastLoginTime;

    /**
     * 是否在线
     */
    @ProtoField(value = 18, desc = "是否在线")
    private boolean online;

    /**
     * VIP 经验
     */
    @ProtoField(value = 19, desc = "VIP经验")
    private long vipExp;

    /**
     * 最大体力
     */
    @ProtoField(value = 20, desc = "最大体力")
    private int maxEnergy;

    /**
     * 个性签名
     */
    @ProtoField(value = 21, desc = "个性签名")
    private String signature;

    /**
     * 创建时间
     */
    @ProtoField(value = 22, desc = "创建时间戳")
    private long createTime;

    /**
     * 是否被封禁
     */
    @ProtoField(value = 23, desc = "是否被封禁")
    private boolean banned;

    /**
     * 封禁结束时间
     */
    @ProtoField(value = 24, desc = "封禁结束时间戳")
    private long banEndTime;
}
