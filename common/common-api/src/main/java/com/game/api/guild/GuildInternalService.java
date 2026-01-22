package com.game.api.guild;

import com.game.common.result.Result;

import java.util.List;

/**
 * 公会内部服务接口
 * <p>
 * 提供给其他服务 (Game/Task/Rank) 内部调用的接口
 * </p>
 *
 * @author GameServer
 */
public interface GuildInternalService {

    // ==================== 玩家相关回调 ====================

    /**
     * 通知公会玩家上线
     *
     * @param roleId  角色 ID
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> onPlayerOnline(long roleId, long guildId);

    /**
     * 通知公会玩家下线
     *
     * @param roleId  角色 ID
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> onPlayerOffline(long roleId, long guildId);

    /**
     * 更新成员战力
     * <p>
     * 由 Game 服务调用，当玩家战力变化时通知公会更新
     * </p>
     *
     * @param roleId      角色 ID
     * @param guildId     公会 ID
     * @param combatPower 战力
     * @return 操作结果
     */
    Result<Void> updateMemberCombatPower(long roleId, long guildId, long combatPower);

    /**
     * 更新成员等级
     *
     * @param roleId  角色 ID
     * @param guildId 公会 ID
     * @param level   等级
     * @return 操作结果
     */
    Result<Void> updateMemberLevel(long roleId, long guildId, int level);

    // ==================== 排行相关 ====================

    /**
     * 获取公会战力
     *
     * @param guildId 公会 ID
     * @return 公会战力
     */
    Result<Long> getGuildCombatPower(long guildId);

    /**
     * 批量获取公会信息
     *
     * @param guildIds 公会 ID 列表
     * @return 公会信息列表
     */
    Result<List<GuildDTO>> batchGetGuilds(List<Long> guildIds);

    // ==================== 定时任务相关 ====================

    /**
     * 触发公会每日重置
     *
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> dailyReset(long guildId);

    /**
     * 触发公会每周重置
     *
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> weeklyReset(long guildId);

    /**
     * 获取所有公会 ID
     *
     * @return 公会 ID 列表
     */
    Result<List<Long>> getAllGuildIds();
}
