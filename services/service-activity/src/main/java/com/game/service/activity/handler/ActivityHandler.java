package com.game.service.activity.handler;

import com.game.api.activity.RewardDTO;
import com.game.entity.document.ActivityConfig;
import com.game.entity.document.PlayerActivity;

import java.util.List;
import java.util.Map;

/**
 * 活动处理器接口
 * <p>
 * 不同类型的活动实现此接口处理具体逻辑
 * </p>
 *
 * @author GameServer
 */
public interface ActivityHandler {

    /**
     * 获取处理器支持的模板ID
     */
    String getTemplateId();

    /**
     * 活动开始时的初始化
     */
    default void onActivityStart(ActivityConfig config) {
    }

    /**
     * 活动结束时的清理
     */
    default void onActivityEnd(ActivityConfig config) {
    }

    /**
     * 玩家首次参与活动时初始化数据
     */
    default void onPlayerInit(ActivityConfig config, PlayerActivity playerActivity) {
    }

    /**
     * 玩家每日重置
     */
    default void onDailyReset(ActivityConfig config, PlayerActivity playerActivity) {
    }

    /**
     * 参与活动
     *
     * @param config         活动配置
     * @param playerActivity 玩家活动数据
     * @param params         参与参数
     * @return 是否成功
     */
    boolean participate(ActivityConfig config, PlayerActivity playerActivity, Map<String, Object> params);

    /**
     * 更新进度
     *
     * @param config         活动配置
     * @param playerActivity 玩家活动数据
     * @param goalId         目标ID
     * @param delta          增量
     * @return 更新后的进度
     */
    long updateProgress(ActivityConfig config, PlayerActivity playerActivity, String goalId, long delta);

    /**
     * 检查奖励是否可领取
     *
     * @param config         活动配置
     * @param playerActivity 玩家活动数据
     * @param rewardId       奖励ID
     * @return 是否可领取
     */
    boolean canClaimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId);

    /**
     * 领取奖励
     *
     * @param config         活动配置
     * @param playerActivity 玩家活动数据
     * @param rewardId       奖励ID
     * @return 奖励内容
     */
    List<RewardDTO> claimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId);

    /**
     * 检查是否有可领取的奖励 (用于红点)
     */
    boolean hasClaimableReward(ActivityConfig config, PlayerActivity playerActivity);

    /**
     * 获取活动规则说明
     */
    default String getRules(ActivityConfig config) {
        return config.getDescription();
    }
}
