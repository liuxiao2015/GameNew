package com.game.service.activity.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.api.activity.RewardDTO;
import com.game.entity.document.ActivityConfig;
import com.game.entity.document.PlayerActivity;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 活动处理器抽象基类
 *
 * @author GameServer
 */
@Slf4j
public abstract class AbstractActivityHandler implements ActivityHandler {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public long updateProgress(ActivityConfig config, PlayerActivity playerActivity, String goalId, long delta) {
        playerActivity.addProgress(goalId, delta);
        return playerActivity.getProgress(goalId);
    }

    @Override
    public boolean canClaimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId) {
        if (playerActivity.isRewardClaimed(rewardId)) {
            return false;
        }
        
        List<RewardConfig> rewards = parseRewards(config);
        for (RewardConfig reward : rewards) {
            if (reward.getRewardId().equals(rewardId)) {
                long progress = playerActivity.getProgress(reward.getGoalId());
                return progress >= reward.getRequiredProgress();
            }
        }
        return false;
    }

    @Override
    public List<RewardDTO> claimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId) {
        if (!canClaimReward(config, playerActivity, rewardId)) {
            return Collections.emptyList();
        }

        List<RewardConfig> rewards = parseRewards(config);
        for (RewardConfig reward : rewards) {
            if (reward.getRewardId().equals(rewardId)) {
                playerActivity.claimReward(rewardId);
                return reward.getRewards();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasClaimableReward(ActivityConfig config, PlayerActivity playerActivity) {
        List<RewardConfig> rewards = parseRewards(config);
        for (RewardConfig reward : rewards) {
            if (!playerActivity.isRewardClaimed(reward.getRewardId())) {
                long progress = playerActivity.getProgress(reward.getGoalId());
                if (progress >= reward.getRequiredProgress()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 解析奖励配置
     */
    protected List<RewardConfig> parseRewards(ActivityConfig config) {
        if (config.getRewardsJson() == null || config.getRewardsJson().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(config.getRewardsJson(), 
                    new TypeReference<List<RewardConfig>>() {});
        } catch (Exception e) {
            log.error("解析奖励配置失败: activityId={}", config.getActivityId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析目标配置
     */
    protected List<GoalConfig> parseGoals(ActivityConfig config) {
        if (config.getGoalsJson() == null || config.getGoalsJson().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(config.getGoalsJson(), 
                    new TypeReference<List<GoalConfig>>() {});
        } catch (Exception e) {
            log.error("解析目标配置失败: activityId={}", config.getActivityId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 奖励配置
     */
    @lombok.Data
    public static class RewardConfig {
        private String rewardId;
        private String name;
        private String goalId;
        private long requiredProgress;
        private List<RewardDTO> rewards;
    }

    /**
     * 目标配置
     */
    @lombok.Data
    public static class GoalConfig {
        private String goalId;
        private String name;
        private String description;
        private long targetValue;
        private String triggerType; // 触发类型
        private Map<String, Object> params;
    }
}
