package com.game.api.guild;

import com.game.common.result.Result;

import java.util.List;

/**
 * 公会服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-guild 模块实现
 * </p>
 *
 * @author GameServer
 */
public interface GuildService {

    /**
     * 创建公会
     *
     * @param roleId      创建者角色 ID
     * @param guildName   公会名
     * @param declaration 公会宣言
     * @param iconId      公会图标 ID
     * @return 公会信息
     */
    Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId);

    /**
     * 获取公会信息
     *
     * @param guildId 公会 ID
     * @return 公会信息
     */
    Result<GuildDTO> getGuildInfo(long guildId);

    /**
     * 获取玩家所在公会信息
     *
     * @param roleId 角色 ID
     * @return 公会信息
     */
    Result<GuildDTO> getPlayerGuild(long roleId);

    /**
     * 搜索公会
     *
     * @param keyword 搜索关键字
     * @param page    页码
     * @param size    每页大小
     * @return 公会列表
     */
    Result<List<GuildDTO>> searchGuild(String keyword, int page, int size);

    /**
     * 申请加入公会
     *
     * @param roleId  申请人角色 ID
     * @param guildId 公会 ID
     * @param message 申请留言
     * @return 操作结果
     */
    Result<Void> applyJoinGuild(long roleId, long guildId, String message);

    /**
     * 直接加入公会 (无需审批)
     *
     * @param roleId  角色 ID
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> joinGuild(long roleId, long guildId);

    /**
     * 处理加入申请
     *
     * @param operatorId 操作人角色 ID
     * @param applyId    申请 ID
     * @param accept     是否同意
     * @return 操作结果
     */
    Result<Void> handleApply(long operatorId, long applyId, boolean accept);

    /**
     * 退出公会
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> leaveGuild(long roleId);

    /**
     * 踢出成员
     *
     * @param operatorId 操作人角色 ID
     * @param targetId   被踢成员角色 ID
     * @return 操作结果
     */
    Result<Void> kickMember(long operatorId, long targetId);

    /**
     * 公会捐献
     *
     * @param roleId     角色 ID
     * @param donateType 捐献类型 (1:金币 2:钻石)
     * @param amount     捐献数量
     * @return 捐献结果 (获得贡献值)
     */
    Result<Long> donate(long roleId, int donateType, long amount);

    /**
     * 修改成员职位
     *
     * @param operatorId  操作人角色 ID
     * @param targetId    目标成员角色 ID
     * @param newPosition 新职位
     * @return 操作结果
     */
    Result<Void> changeMemberPosition(long operatorId, long targetId, int newPosition);

    /**
     * 转让会长
     *
     * @param currentLeaderId 当前会长角色 ID
     * @param newLeaderId     新会长角色 ID
     * @return 操作结果
     */
    Result<Void> transferLeader(long currentLeaderId, long newLeaderId);

    /**
     * 解散公会
     *
     * @param leaderId 会长角色 ID
     * @return 操作结果
     */
    Result<Void> dissolveGuild(long leaderId);

    /**
     * 获取公会成员列表
     *
     * @param guildId 公会 ID
     * @return 成员列表
     */
    Result<List<GuildMemberDTO>> getMembers(long guildId);

    /**
     * 每日重置
     * <p>
     * 由调度服务调用，重置所有公会的每日数据
     * </p>
     *
     * @return 操作结果
     */
    Result<Void> dailyReset();
}
