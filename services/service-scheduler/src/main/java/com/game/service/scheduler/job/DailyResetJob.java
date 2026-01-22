package com.game.service.scheduler.job;

import com.game.api.player.PlayerService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每日重置任务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyResetJob {

    private final PlayerService playerService;

    /**
     * 每日零点重置
     * XXL-Job 配置: 0 0 0 * * ?
     */
    @XxlJob("dailyResetHandler")
    public void dailyReset() {
        XxlJobHelper.log("开始执行每日重置任务");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 重置每日任务
            resetDailyTask();

            // 2. 重置每日副本次数
            resetDungeonCount();

            // 3. 重置每日商店
            resetDailyShop();

            // 4. 重置签到状态
            resetSignIn();

            // 5. 发送每日奖励
            sendDailyReward();

            long costTime = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("每日重置任务完成, 耗时: {}ms", costTime);
            log.info("每日重置任务完成, 耗时: {}ms", costTime);

        } catch (Exception e) {
            XxlJobHelper.log("每日重置任务异常: {}", e.getMessage());
            log.error("每日重置任务异常", e);
            XxlJobHelper.handleFail("每日重置任务异常: " + e.getMessage());
        }
    }

    private void resetDailyTask() {
        XxlJobHelper.log("重置每日任务...");
        // 实际实现: 清理玩家每日任务进度
    }

    private void resetDungeonCount() {
        XxlJobHelper.log("重置副本次数...");
        // 实际实现: 重置玩家副本挑战次数
    }

    private void resetDailyShop() {
        XxlJobHelper.log("重置每日商店...");
        // 实际实现: 刷新商店物品
    }

    private void resetSignIn() {
        XxlJobHelper.log("重置签到状态...");
        // 实际实现: 标记签到状态为未签到
    }

    private void sendDailyReward() {
        XxlJobHelper.log("发送每日奖励...");
        // 实际实现: 发送在线奖励、VIP 奖励等
    }
}
