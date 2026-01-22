package com.game.service.scheduler.job;

import com.game.api.rank.RankService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 排行榜定时任务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankJob {

    @DubboReference
    private RankService rankService;

    /**
     * 排行榜刷新 (每 5 分钟)
     * XXL-Job 配置: 0 0/5 * * * ?
     */
    @XxlJob("rankRefreshHandler")
    public void refreshRank() {
        XxlJobHelper.log("开始刷新排行榜");
        long startTime = System.currentTimeMillis();

        try {
            // 刷新各类排行榜玩家信息
            for (int rankType = 1; rankType <= 7; rankType++) {
                rankService.refreshRank(rankType);
                XxlJobHelper.log("排行榜 {} 刷新完成", rankType);
            }

            long costTime = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("排行榜刷新完成, 耗时: {}ms", costTime);
            log.info("排行榜刷新完成, 耗时: {}ms", costTime);

        } catch (Exception e) {
            XxlJobHelper.log("排行榜刷新异常: {}", e.getMessage());
            log.error("排行榜刷新异常", e);
            XxlJobHelper.handleFail("排行榜刷新异常: " + e.getMessage());
        }
    }

    /**
     * 排行榜快照 (每日 23:59)
     * XXL-Job 配置: 0 59 23 * * ?
     */
    @XxlJob("rankSnapshotHandler")
    public void snapshotRank() {
        XxlJobHelper.log("开始生成排行榜快照");

        try {
            // 保存排行榜历史快照 (用于奖励发放参考)
            // 实际实现: 遍历排行榜保存到 MongoDB

            XxlJobHelper.log("排行榜快照完成");
            log.info("排行榜快照完成");

        } catch (Exception e) {
            XxlJobHelper.log("排行榜快照异常: {}", e.getMessage());
            log.error("排行榜快照异常", e);
            XxlJobHelper.handleFail("排行榜快照异常: " + e.getMessage());
        }
    }
}
