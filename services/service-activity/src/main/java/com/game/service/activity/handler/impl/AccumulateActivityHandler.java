package com.game.service.activity.handler.impl;

import com.game.entity.document.ActivityConfig;
import com.game.entity.document.PlayerActivity;
import com.game.service.activity.handler.AbstractActivityHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 累计活动处理器
 * <p>
 * 支持累计充值、累计消费、累计登录等
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class AccumulateActivityHandler extends AbstractActivityHandler {

    private static final String TEMPLATE_ID = "accumulate";

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public boolean participate(ActivityConfig config, PlayerActivity playerActivity, Map<String, Object> params) {
        // 累计活动通过 updateProgress 更新
        return true;
    }

    @Override
    public long updateProgress(ActivityConfig config, PlayerActivity playerActivity, String goalId, long delta) {
        long newProgress = super.updateProgress(config, playerActivity, goalId, delta);
        log.debug("累计活动进度更新: roleId={}, activityId={}, goalId={}, delta={}, total={}",
                playerActivity.getRoleId(), config.getActivityId(), goalId, delta, newProgress);
        return newProgress;
    }
}
