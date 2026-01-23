package com.game.api.player;

import com.game.common.result.Result;

/**
 * 玩家服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-game 模块实现
 * </p>
 *
 * @author GameServer
 */
public interface PlayerService {

    /**
     * 获取玩家信息
     *
     * @param roleId 角色 ID
     * @return 玩家信息
     */
    Result<PlayerDTO> getPlayerInfo(long roleId);

    /**
     * 增加经验
     *
     * @param roleId 角色 ID
     * @param exp    经验值
     * @param reason 原因
     * @return 操作结果
     */
    Result<Void> addExp(long roleId, long exp, String reason);

    /**
     * 增加金币
     *
     * @param roleId 角色 ID
     * @param gold   金币数量
     * @param reason 原因
     * @return 操作结果
     */
    Result<Void> addGold(long roleId, long gold, String reason);

    /**
     * 增加钻石
     *
     * @param roleId  角色 ID
     * @param diamond 钻石数量
     * @param reason  原因
     * @return 操作结果
     */
    Result<Void> addDiamond(long roleId, long diamond, String reason);

    /**
     * 增加物品
     *
     * @param roleId 角色 ID
     * @param itemId 物品配置 ID
     * @param count  数量
     * @param reason 原因
     * @return 操作结果
     */
    Result<Void> addItem(long roleId, int itemId, long count, String reason);

    /**
     * 修改玩家名字
     *
     * @param roleId  角色 ID
     * @param newName 新名字
     * @return 操作结果
     */
    Result<Void> changeName(long roleId, String newName);

    /**
     * 修改头像
     *
     * @param roleId   角色 ID
     * @param avatarId 头像 ID
     * @return 操作结果
     */
    Result<Void> changeAvatar(long roleId, int avatarId);

    /**
     * 封禁玩家
     *
     * @param roleId   角色 ID
     * @param duration 封禁时长 (秒，0 表示永久)
     * @param reason   封禁原因
     * @return 操作结果
     */
    Result<Void> banPlayer(long roleId, long duration, String reason);

    /**
     * 解封玩家
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> unbanPlayer(long roleId);

    /**
     * 设置公会信息
     *
     * @param roleId    角色 ID
     * @param guildId   公会 ID
     * @param guildName 公会名称
     * @param position  职位
     * @return 操作结果
     */
    Result<Void> setGuildInfo(long roleId, long guildId, String guildName, int position);

    /**
     * 每日重置
     * <p>
     * 由调度服务调用，重置所有玩家的每日数据
     * </p>
     *
     * @return 操作结果
     */
    Result<Void> dailyReset();
}
