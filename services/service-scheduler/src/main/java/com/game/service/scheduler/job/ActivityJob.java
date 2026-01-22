package com.game.service.scheduler.job;

import com.game.data.redis.RedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 活动相关定时任务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityJob {

    private final RedisService redisService;

    /**
     * 活动状态检查 (每分钟)
     * XXL-Job 配置: 0 * * * * ?
     */
    @XxlJob("activityCheckHandler")
    public void checkActivityStatus() {
        try {
            // 检查待开启的活动
            checkPendingActivities();

            // 检查待结束的活动
            checkEndingActivities();

        } catch (Exception e) {
            log.error("活动状态检查异常", e);
            XxlJobHelper.handleFail("活动状态检查异常: " + e.getMessage());
        }
    }

    private void checkPendingActivities() {
        // 检查是否有活动需要开启
        // 实际实现: 查询活动配置，检查开始时间
    }

    private void checkEndingActivities() {
        // 检查是否有活动需要结束
        // 实际实现: 查询活动配置，检查结束时间，发放奖励
    }

    /**
     * 限时活动结算
     * XXL-Job 配置: 动态配置
     */
    @XxlJob("activitySettleHandler")
    public void settleActivity() {
        String activityId = XxlJobHelper.getJobParam();
        XxlJobHelper.log("开始结算活动: {}", activityId);

        try {
            // 根据活动 ID 结算
            // 实际实现: 计算排名，发放奖励

            XxlJobHelper.log("活动结算完成: {}", activityId);
            log.info("活动结算完成: {}", activityId);

        } catch (Exception e) {
            XxlJobHelper.log("活动结算异常: {}", e.getMessage());
            log.error("活动结算异常: activityId={}", activityId, e);
            XxlJobHelper.handleFail("活动结算异常: " + e.getMessage());
        }
    }
}
