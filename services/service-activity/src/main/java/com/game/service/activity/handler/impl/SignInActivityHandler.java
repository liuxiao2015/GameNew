package com.game.service.activity.handler.impl;

import com.game.api.activity.RewardDTO;
import com.game.entity.document.ActivityConfig;
import com.game.entity.document.PlayerActivity;
import com.game.service.activity.handler.AbstractActivityHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 签到活动处理器
 * <p>
 * 支持每日签到、累计签到、连续签到
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class SignInActivityHandler extends AbstractActivityHandler {

    private static final String TEMPLATE_ID = "sign_in";
    private static final String GOAL_DAILY = "daily_sign";
    private static final String GOAL_CUMULATIVE = "cumulative_sign";
    private static final String GOAL_CONSECUTIVE = "consecutive_sign";
    private static final String EXTRA_LAST_SIGN_DATE = "lastSignDate";

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public void onPlayerInit(ActivityConfig config, PlayerActivity playerActivity) {
        playerActivity.setProgress(GOAL_DAILY, 0);
        playerActivity.setProgress(GOAL_CUMULATIVE, 0);
        playerActivity.setProgress(GOAL_CONSECUTIVE, 0);
    }

    @Override
    public void onDailyReset(ActivityConfig config, PlayerActivity playerActivity) {
        // 每日签到状态重置
        playerActivity.setProgress(GOAL_DAILY, 0);
        
        // 检查连续签到是否断签
        String lastSignDate = playerActivity.getExtra(EXTRA_LAST_SIGN_DATE, "");
        if (!lastSignDate.isEmpty()) {
            LocalDate last = LocalDate.parse(lastSignDate);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            if (!last.equals(yesterday)) {
                // 断签，重置连续签到
                playerActivity.setProgress(GOAL_CONSECUTIVE, 0);
                log.info("连续签到断签: roleId={}, activityId={}", 
                        playerActivity.getRoleId(), playerActivity.getActivityId());
            }
        }
    }

    @Override
    public boolean participate(ActivityConfig config, PlayerActivity playerActivity, Map<String, Object> params) {
        // 检查今日是否已签到
        if (playerActivity.getProgress(GOAL_DAILY) > 0) {
            log.info("今日已签到: roleId={}", playerActivity.getRoleId());
            return false;
        }

        String today = LocalDate.now().toString();
        
        // 更新签到数据
        playerActivity.setProgress(GOAL_DAILY, 1);
        playerActivity.addProgress(GOAL_CUMULATIVE, 1);
        playerActivity.addProgress(GOAL_CONSECUTIVE, 1);
        playerActivity.setExtra(EXTRA_LAST_SIGN_DATE, today);
        playerActivity.setLastParticipateTime(java.time.LocalDateTime.now());
        playerActivity.setParticipateCount(playerActivity.getParticipateCount() + 1);

        log.info("签到成功: roleId={}, cumulative={}, consecutive={}", 
                playerActivity.getRoleId(), 
                playerActivity.getProgress(GOAL_CUMULATIVE),
                playerActivity.getProgress(GOAL_CONSECUTIVE));

        return true;
    }

    @Override
    public List<RewardDTO> claimReward(ActivityConfig config, PlayerActivity playerActivity, String rewardId) {
        List<RewardDTO> rewards = super.claimReward(config, playerActivity, rewardId);
        if (!rewards.isEmpty()) {
            log.info("领取签到奖励: roleId={}, rewardId={}", playerActivity.getRoleId(), rewardId);
        }
        return rewards;
    }
}
