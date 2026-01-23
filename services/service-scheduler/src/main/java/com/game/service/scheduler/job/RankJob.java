package com.game.service.scheduler.job;

import com.game.api.rank.RankSnapshotService;
import com.game.common.result.Result;
import com.game.core.rank.RankService;
import com.game.data.redis.RedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 排行榜定时任务
 * <p>
 * 演示排行榜相关的定时任务：
 * <ul>
 *     <li>定时刷新排行榜数据</li>
 *     <li>每日保存排行榜快照</li>
 *     <li>每周/每月重置排行榜</li>
 *     <li>排行榜奖励发放</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankJob {

    private final RankService rankService;
    private final RedisService redisService;

    @DubboReference(check = false)
    private RankSnapshotService rankSnapshotService;

    /**
     * 刷新排行榜 - 每小时执行
     * XXL-Job 配置: 0 0 * * * ?
     */
    @XxlJob("refreshRankHandler")
    public void refreshRank() {
        XxlJobHelper.log("开始刷新排行榜");
        long startTime = System.currentTimeMillis();

        try {
            // 刷新各类排行榜
            refreshCombatPowerRank();
            refreshLevelRank();
            refreshGuildRank();

            // 更新刷新时间
            long now = System.currentTimeMillis();
            redisService.setString("rank:refresh:all", String.valueOf(now));

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
     * 每日排行榜快照 - 每天 23:55 执行
     * XXL-Job 配置: 0 55 23 * * ?
     */
    @XxlJob("dailyRankSnapshotHandler")
    public void dailyRankSnapshot() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        XxlJobHelper.log("开始保存每日排行榜快照: {}", today);

        try {
            // 保存各类排行榜快照 (1=战力榜, 2=等级榜, 7=公会榜)
            saveRankSnapshot(1, "daily_power_" + today);
            saveRankSnapshot(2, "daily_level_" + today);
            saveRankSnapshot(7, "daily_guild_" + today);

            XxlJobHelper.log("每日排行榜快照保存完成");
            log.info("每日排行榜快照保存完成: {}", today);

        } catch (Exception e) {
            XxlJobHelper.log("排行榜快照保存异常: {}", e.getMessage());
            log.error("排行榜快照保存异常", e);
        }
    }

    /**
     * 每周排行榜重置 - 每周一 00:05 执行
     * XXL-Job 配置: 0 5 0 ? * MON
     */
    @XxlJob("weeklyRankResetHandler")
    public void weeklyRankReset() {
        XxlJobHelper.log("开始执行每周排行榜重置");

        try {
            // 1. 先保存快照 (6=竞技场榜)
            String weekStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
            saveRankSnapshot(6, "weekly_arena_" + weekStr);

            // 2. 发放排行奖励
            sendWeeklyRankReward(6);

            // 3. 清空排行榜
            rankService.clear("rank:arena");

            XxlJobHelper.log("每周排行榜重置完成");
            log.info("每周排行榜重置完成");

        } catch (Exception e) {
            XxlJobHelper.log("每周排行榜重置异常: {}", e.getMessage());
            log.error("每周排行榜重置异常", e);
        }
    }

    /**
     * 排行榜数据清理 - 每月 1 日 04:00 执行
     * XXL-Job 配置: 0 0 4 1 * ?
     */
    @XxlJob("cleanRankDataHandler")
    public void cleanRankData() {
        XxlJobHelper.log("开始清理过期排行榜数据");

        try {
            // 删除 30 天前的快照
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            String cutoffDateStr = cutoffDate.format(DateTimeFormatter.ISO_DATE);

            if (rankSnapshotService != null) {
                Result<Integer> result = rankSnapshotService.deleteSnapshotsBefore(cutoffDateStr);
                if (result.isSuccess()) {
                    XxlJobHelper.log("清理过期快照: {} 条", result.getData());
                }
            }

            // 修剪排行榜，只保留 Top 10000
            rankService.trim(RankService.RANK_COMBAT_POWER, 10000);
            rankService.trim(RankService.RANK_LEVEL, 10000);

            XxlJobHelper.log("排行榜数据清理完成");
            log.info("排行榜数据清理完成");

        } catch (Exception e) {
            XxlJobHelper.log("排行榜数据清理异常: {}", e.getMessage());
            log.error("排行榜数据清理异常", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 刷新战力排行榜
     */
    private void refreshCombatPowerRank() {
        // 实际实现：从数据库批量查询玩家战力并更新到 Redis
        XxlJobHelper.log("刷新战力排行榜");
    }

    /**
     * 刷新等级排行榜
     */
    private void refreshLevelRank() {
        // 实际实现：从数据库批量查询玩家等级并更新到 Redis
        XxlJobHelper.log("刷新等级排行榜");
    }

    /**
     * 刷新公会排行榜
     */
    private void refreshGuildRank() {
        // 实际实现：从数据库批量查询公会战力并更新到 Redis
        XxlJobHelper.log("刷新公会排行榜");
    }

    /**
     * 保存排行榜快照
     */
    private void saveRankSnapshot(int rankType, String snapshotName) {
        try {
            if (rankSnapshotService != null) {
                Result<Void> result = rankSnapshotService.saveSnapshot(rankType, snapshotName);
                if (result.isSuccess()) {
                    XxlJobHelper.log("保存快照成功: {}", snapshotName);
                }
            }
        } catch (Exception e) {
            log.error("保存排行榜快照失败: {}", snapshotName, e);
        }
    }

    /**
     * 发放每周排行奖励
     */
    private void sendWeeklyRankReward(int rankType) {
        XxlJobHelper.log("发放每周排行奖励: rankType={}", rankType);

        // 获取 Top 100 玩家
        var topList = rankService.getTopN("rank:arena", 100);

        for (var entry : topList) {
            int rank = entry.rank();
            long roleId = entry.memberId();

            // 根据排名发放奖励 (通过邮件)
            // 实际实现：调用邮件服务发送奖励邮件

            if (rank <= 10) {
                // 前 10 名奖励
                log.debug("发放 Top10 奖励: roleId={}, rank={}", roleId, rank);
            } else if (rank <= 50) {
                // 11-50 名奖励
                log.debug("发放 Top50 奖励: roleId={}, rank={}", roleId, rank);
            } else {
                // 51-100 名奖励
                log.debug("发放 Top100 奖励: roleId={}, rank={}", roleId, rank);
            }
        }
    }
}
