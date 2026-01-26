package com.game.service.activity.handler.impl;

import com.game.entity.document.ActivityConfig;
import com.game.entity.document.ActivityRank;
import com.game.entity.document.PlayerActivity;
import com.game.entity.repository.ActivityRankRepository;
import com.game.service.activity.handler.AbstractActivityHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 排行活动处理器
 * <p>
 * 支持冲榜活动，如战力榜、等级榜、充值榜
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankActivityHandler extends AbstractActivityHandler {

    private static final String TEMPLATE_ID = "rank";
    private static final String GOAL_SCORE = "score";

    private final ActivityRankRepository rankRepository;

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public boolean participate(ActivityConfig config, PlayerActivity playerActivity, Map<String, Object> params) {
        // 排行活动通过 updateProgress 更新分数
        return true;
    }

    @Override
    public long updateProgress(ActivityConfig config, PlayerActivity playerActivity, String goalId, long delta) {
        long newScore = super.updateProgress(config, playerActivity, goalId, delta);
        
        // 同步更新排行榜
        if (GOAL_SCORE.equals(goalId)) {
            updateRank(config, playerActivity, newScore);
        }
        
        return newScore;
    }

    /**
     * 更新排行榜
     */
    private void updateRank(ActivityConfig config, PlayerActivity playerActivity, long score) {
        ActivityRank rank = rankRepository.findByActivityIdAndActivityVersionAndRoleId(
                config.getActivityId(), config.getActivityVersion(), playerActivity.getRoleId())
                .orElseGet(() -> {
                    ActivityRank newRank = new ActivityRank();
                    newRank.setActivityId(config.getActivityId());
                    newRank.setActivityVersion(config.getActivityVersion());
                    newRank.setRoleId(playerActivity.getRoleId());
                    return newRank;
                });

        rank.setScore(score);
        rank.setLastUpdateTime(LocalDateTime.now());
        rankRepository.save(rank);

        log.debug("更新活动排行: roleId={}, activityId={}, score={}",
                playerActivity.getRoleId(), config.getActivityId(), score);
    }

    @Override
    public void onActivityEnd(ActivityConfig config) {
        // 活动结束时计算最终排名
        log.info("排行活动结束，计算最终排名: activityId={}", config.getActivityId());
        // TODO: 计算排名并发放排名奖励
    }
}
