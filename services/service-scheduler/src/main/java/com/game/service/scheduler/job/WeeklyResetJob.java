package com.game.service.scheduler.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每周重置任务
 *
 * @author GameServer
 */
@Slf4j
@Component
public class WeeklyResetJob {

    /**
     * 每周一零点重置
     * XXL-Job 配置: 0 0 0 ? * MON
     */
    @XxlJob("weeklyResetHandler")
    public void weeklyReset() {
        XxlJobHelper.log("开始执行每周重置任务");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 重置周任务
            resetWeeklyTask();

            // 2. 结算竞技场赛季奖励
            settleArenaReward();

            // 3. 重置周常活动
            resetWeeklyActivity();

            // 4. 公会周结算
            settleGuildWeekly();

            long costTime = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("每周重置任务完成, 耗时: {}ms", costTime);
            log.info("每周重置任务完成, 耗时: {}ms", costTime);

        } catch (Exception e) {
            XxlJobHelper.log("每周重置任务异常: {}", e.getMessage());
            log.error("每周重置任务异常", e);
            XxlJobHelper.handleFail("每周重置任务异常: " + e.getMessage());
        }
    }

    private void resetWeeklyTask() {
        XxlJobHelper.log("重置周任务...");
    }

    private void settleArenaReward() {
        XxlJobHelper.log("结算竞技场奖励...");
    }

    private void resetWeeklyActivity() {
        XxlJobHelper.log("重置周常活动...");
    }

    private void settleGuildWeekly() {
        XxlJobHelper.log("公会周结算...");
    }
}
