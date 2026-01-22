package com.game.api.player;

import com.game.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 玩家内部服务接口
 * <p>
 * 提供给其他服务 (Guild/Task/Rank) 内部调用的接口
 * 与 PlayerService 区分，用于服务间通信
 * </p>
 *
 * @author GameServer
 */
public interface PlayerInternalService {

    // ==================== 公会相关回调 ====================

    /**
     * 设置玩家公会信息
     * <p>
     * 由 Guild 服务调用，当玩家加入/退出公会时同步更新玩家数据
     * </p>
     *
     * @param roleId    角色 ID
     * @param guildId   公会 ID (0 表示退出公会)
     * @param guildName 公会名称
     * @param position  职位
     * @return 操作结果
     */
    Result<Void> setPlayerGuild(long roleId, long guildId, String guildName, int position);

    /**
     * 扣除玩家货币 (用于公会捐献等)
     *
     * @param roleId       角色 ID
     * @param currencyType 货币类型 (1:金币 2:钻石)
     * @param amount       数量
     * @param reason       原因
     * @return 操作结果
     */
    Result<Void> deductCurrency(long roleId, int currencyType, long amount, String reason);

    /**
     * 检查玩家是否有足够货币
     *
     * @param roleId       角色 ID
     * @param currencyType 货币类型
     * @param amount       数量
     * @return true 表示足够
     */
    Result<Boolean> checkCurrency(long roleId, int currencyType, long amount);

    // ==================== 排行相关 ====================

    /**
     * 获取玩家战力
     *
     * @param roleId 角色 ID
     * @return 战力值
     */
    Result<Long> getPlayerCombatPower(long roleId);

    /**
     * 批量获取玩家简要信息
     * <p>
     * 用于排行榜展示
     * </p>
     *
     * @param roleIds 角色 ID 列表
     * @return 玩家信息列表
     */
    Result<List<PlayerDTO>> batchGetPlayers(List<Long> roleIds);

    // ==================== 任务/活动相关 ====================

    /**
     * 触发玩家每日重置
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> dailyReset(long roleId);

    /**
     * 触发玩家每周重置
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> weeklyReset(long roleId);

    /**
     * 发送邮件给玩家
     *
     * @param roleId      角色 ID
     * @param title       标题
     * @param content     内容
     * @param attachments 附件 Map<物品ID, 数量>
     * @return 操作结果
     */
    Result<Void> sendMail(long roleId, String title, String content, Map<Integer, Long> attachments);

    // ==================== 在线状态 ====================

    /**
     * 检查玩家是否在线
     *
     * @param roleId 角色 ID
     * @return true 表示在线
     */
    Result<Boolean> isOnline(long roleId);

    /**
     * 获取所有在线玩家 ID
     *
     * @return 在线玩家 ID 列表
     */
    Result<List<Long>> getOnlinePlayers();
}
