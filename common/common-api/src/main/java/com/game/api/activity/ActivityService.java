package com.game.api.activity;

import com.game.common.result.Result;

import java.util.List;

/**
 * 活动服务接口 (Dubbo RPC)
 *
 * @author GameServer
 */
public interface ActivityService {

    /**
     * 获取活动列表
     *
     * @param roleId   角色ID
     * @param serverId 服务器ID
     * @return 活动列表
     */
    Result<List<ActivityDTO>> getActivityList(long roleId, int serverId);

    /**
     * 获取活动详情
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @return 活动详情
     */
    Result<ActivityDetailDTO> getActivityDetail(long roleId, int activityId);

    /**
     * 获取玩家活动进度
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @return 活动进度
     */
    Result<PlayerActivityDTO> getPlayerProgress(long roleId, int activityId);

    /**
     * 领取活动奖励
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @param rewardId   奖励ID
     * @return 奖励内容
     */
    Result<List<RewardDTO>> claimReward(long roleId, int activityId, String rewardId);

    /**
     * 更新活动进度
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @param goalId     目标ID
     * @param delta      增量
     * @return 更新后的进度
     */
    Result<Long> updateProgress(long roleId, int activityId, String goalId, long delta);

    /**
     * 参与活动
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @param params     参与参数
     * @return 操作结果
     */
    Result<Void> participate(long roleId, int activityId, String params);

    /**
     * 获取活动排行榜
     *
     * @param activityId 活动ID
     * @param page       页码
     * @param size       每页数量
     * @return 排行榜数据
     */
    Result<ActivityRankDTO> getActivityRank(int activityId, int page, int size);

    /**
     * 获取玩家在活动中的排名
     *
     * @param roleId     角色ID
     * @param activityId 活动ID
     * @return 排名信息
     */
    Result<PlayerRankDTO> getPlayerRank(long roleId, int activityId);

    /**
     * 每日重置通知 (由调度服务调用)
     */
    Result<Void> dailyReset();
}
