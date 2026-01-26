package com.game.service.activity.scheduler;

import com.game.entity.document.ActivityConfig;
import com.game.entity.document.ActivityStatus;
import com.game.entity.repository.ActivityConfigRepository;
import com.game.service.activity.handler.ActivityHandler;
import com.game.service.activity.handler.ActivityHandlerFactory;
import com.game.service.activity.manager.ActivityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动调度器
 * <p>
 * 定时检查活动状态，自动开启/关闭活动
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityScheduler {

    private final ActivityManager activityManager;
    private final ActivityConfigRepository configRepository;
    private final ActivityHandlerFactory handlerFactory;

    @Value("${activity.preview-minutes:30}")
    private int previewMinutes;

    /**
     * 定时检查活动状态 (每分钟)
     */
    @Scheduled(fixedRate = 60000)
    public void checkActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 检查需要进入预告状态的活动
        checkPreviewActivities(now);

        // 2. 检查需要开始的活动
        checkStartActivities(now);

        // 3. 检查需要结束的活动
        checkEndActivities(now);

        // 4. 检查需要关闭的活动
        checkCloseActivities(now);
    }

    /**
     * 检查预告活动
     */
    private void checkPreviewActivities(LocalDateTime now) {
        LocalDateTime previewTime = now.plusMinutes(previewMinutes);
        
        List<ActivityConfig> activities = configRepository.findByStatusAndStartTimeBetween(
                ActivityStatus.NOT_STARTED, now, previewTime);

        for (ActivityConfig config : activities) {
            config.setStatus(ActivityStatus.PREVIEW);
            configRepository.save(config);
            activityManager.refreshActivity(config.getActivityId());
            log.info("活动进入预告: activityId={}, name={}, startTime={}",
                    config.getActivityId(), config.getName(), config.getStartTime());
        }
    }

    /**
     * 检查开始活动
     */
    private void checkStartActivities(LocalDateTime now) {
        // 查询未开始但已到开始时间的活动
        for (ActivityConfig config : activityManager.getAllActivities()) {
            if (config.getStatus() == ActivityStatus.NOT_STARTED || 
                config.getStatus() == ActivityStatus.PREVIEW) {
                
                if (config.getStartTime() != null && !now.isBefore(config.getStartTime())) {
                    startActivity(config);
                }
            }
        }
    }

    /**
     * 检查结束活动
     */
    private void checkEndActivities(LocalDateTime now) {
        for (ActivityConfig config : activityManager.getRunningActivities()) {
            if (config.getEndTime() != null && now.isAfter(config.getEndTime())) {
                endActivity(config);
            }
        }
    }

    /**
     * 检查关闭活动 (领奖期结束)
     */
    private void checkCloseActivities(LocalDateTime now) {
        List<ActivityConfig> endedActivities = configRepository.findByStatus(ActivityStatus.ENDED);
        
        for (ActivityConfig config : endedActivities) {
            LocalDateTime closeTime = config.getRewardDeadline();
            if (closeTime == null) {
                closeTime = config.getEndTime();
            }
            
            if (closeTime != null && now.isAfter(closeTime)) {
                closeActivity(config);
            }
        }
    }

    /**
     * 启动活动
     */
    private void startActivity(ActivityConfig config) {
        config.setStatus(ActivityStatus.RUNNING);
        configRepository.save(config);
        activityManager.refreshActivity(config.getActivityId());

        // 调用活动处理器的启动回调
        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        if (handler != null) {
            try {
                handler.onActivityStart(config);
            } catch (Exception e) {
                log.error("活动启动回调异常: activityId={}", config.getActivityId(), e);
            }
        }

        log.info("活动已开始: activityId={}, name={}", config.getActivityId(), config.getName());
    }

    /**
     * 结束活动
     */
    private void endActivity(ActivityConfig config) {
        config.setStatus(ActivityStatus.ENDED);
        configRepository.save(config);
        activityManager.refreshActivity(config.getActivityId());

        // 调用活动处理器的结束回调
        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        if (handler != null) {
            try {
                handler.onActivityEnd(config);
            } catch (Exception e) {
                log.error("活动结束回调异常: activityId={}", config.getActivityId(), e);
            }
        }

        log.info("活动已结束: activityId={}, name={}", config.getActivityId(), config.getName());
    }

    /**
     * 关闭活动
     */
    private void closeActivity(ActivityConfig config) {
        config.setStatus(ActivityStatus.CLOSED);
        configRepository.save(config);
        activityManager.refreshActivity(config.getActivityId());

        log.info("活动已关闭: activityId={}, name={}", config.getActivityId(), config.getName());
    }
}
