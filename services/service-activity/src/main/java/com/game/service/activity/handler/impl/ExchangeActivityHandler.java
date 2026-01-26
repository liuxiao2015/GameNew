package com.game.service.activity.handler.impl;

import com.game.api.activity.RewardDTO;
import com.game.entity.document.ActivityConfig;
import com.game.entity.document.PlayerActivity;
import com.game.service.activity.handler.AbstractActivityHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 兑换活动处理器
 * <p>
 * 支持道具兑换、积分兑换等
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class ExchangeActivityHandler extends AbstractActivityHandler {

    private static final String TEMPLATE_ID = "exchange";
    private static final String GOAL_CURRENCY = "currency"; // 兑换货币/积分

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public boolean participate(ActivityConfig config, PlayerActivity playerActivity, Map<String, Object> params) {
        // 兑换活动通过 claimReward 进行
        return true;
    }

    @Override
    public boolean canClaimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId) {
        // 检查是否已达到兑换次数限制
        int exchangeCount = playerActivity.getExtra("exchange_" + rewardId, 0);
        
        List<AbstractActivityHandler.RewardConfig> rewards = parseRewards(config);
        for (AbstractActivityHandler.RewardConfig reward : rewards) {
            if (reward.getRewardId().equals(rewardId)) {
                // 检查货币是否足够
                long currency = playerActivity.getProgress(GOAL_CURRENCY);
                return currency >= reward.getRequiredProgress();
            }
        }
        return false;
    }

    @Override
    public List<RewardDTO> claimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId) {
        if (!canClaimReward(config, playerActivity, rewardId)) {
            return Collections.emptyList();
        }

        List<AbstractActivityHandler.RewardConfig> rewards = parseRewards(config);
        for (AbstractActivityHandler.RewardConfig reward : rewards) {
            if (reward.getRewardId().equals(rewardId)) {
                // 扣除货币
                long cost = reward.getRequiredProgress();
                playerActivity.addProgress(GOAL_CURRENCY, -cost);
                
                // 增加兑换次数
                int exchangeCount = playerActivity.getExtra("exchange_" + rewardId, 0);
                playerActivity.setExtra("exchange_" + rewardId, exchangeCount + 1);
                
                log.info("兑换成功: roleId={}, rewardId={}, cost={}", 
                        playerActivity.getRoleId(), rewardId, cost);
                
                return reward.getRewards();
            }
        }
        return Collections.emptyList();
    }
}
