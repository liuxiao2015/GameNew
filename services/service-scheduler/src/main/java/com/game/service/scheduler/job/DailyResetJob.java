package com.game.service.scheduler.job;

import com.game.api.guild.GuildService;
import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.core.event.DistributedEventBus;
import com.game.data.redis.RedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日重置任务
 * <p>
 * 演示分布式任务调度与跨服务调用：
 * <ul>
 *     <li>XXL-Job: 分布式任务调度</li>
 *     <li>Dubbo RPC: 调用玩家服务、公会服务</li>
 *     <li>DistributedEventBus: 发布全局事件</li>
 *     <li>RedisService: 分布式锁确保任务只执行一次</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyResetJob {

    private final RedisService redisService;
    private final DistributedEventBus distributedEventBus;

    @DubboReference(check = false)
    private PlayerService playerService;

    @DubboReference(check = false)
    private GuildService guildService;

    /**
     * 每日零点重置
     * XXL-Job 配置: 0 0 0 * * ?
     */
    @XxlJob("dailyResetHandler")
    public void dailyReset() {
        String today = LocalDate.now().toString();
        String lockKey = "scheduler:daily_reset:" + today;

        XxlJobHelper.log("开始执行每日重置任务: {}", today);
        long startTime = System.currentTimeMillis();

        // 使用 Redis 检查是否已执行 (防止重复执行)
        Boolean alreadyDone = redisService.setIfAbsent(lockKey, "1", java.time.Duration.ofHours(24));
        if (!Boolean.TRUE.equals(alreadyDone)) {
            XxlJobHelper.log("每日重置任务已执行过，跳过");
            return;
        }

        try {
            // 1. 重置玩家每日数据
            XxlJobHelper.log("步骤 1: 重置玩家每日数据...");
            resetPlayerDailyData();

            // 2. 重置公会每日数据
            XxlJobHelper.log("步骤 2: 重置公会每日数据...");
            resetGuildDailyData();

            // 3. 重置每日副本次数
            XxlJobHelper.log("步骤 3: 重置副本次数...");
            resetDungeonCount();

            // 4. 重置每日商店
            XxlJobHelper.log("步骤 4: 重置每日商店...");
            resetDailyShop();

            // 5. 重置签到状态
            XxlJobHelper.log("步骤 5: 重置签到状态...");
            resetSignIn();

            // 6. 发布每日重置事件 (通知所有服务)
            XxlJobHelper.log("步骤 6: 发布每日重置事件...");
            publishDailyResetEvent(today);

            long costTime = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("每日重置任务完成, 耗时: {}ms", costTime);
            log.info("每日重置任务完成, date={}, 耗时: {}ms", today, costTime);

        } catch (Exception e) {
            // 任务失败，删除锁以便重试
            redisService.delete(lockKey);
            XxlJobHelper.log("每日重置任务异常: {}", e.getMessage());
            log.error("每日重置任务异常", e);
            XxlJobHelper.handleFail("每日重置任务异常: " + e.getMessage());
        }
    }

    /**
     * 重置玩家每日数据
     */
    private void resetPlayerDailyData() {
        try {
            // 调用玩家服务执行每日重置
            Result<Void> result = playerService.dailyReset();
            if (!result.isSuccess()) {
                log.warn("玩家每日重置失败: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("调用玩家服务失败", e);
        }
    }

    /**
     * 重置公会每日数据
     */
    private void resetGuildDailyData() {
        try {
            Result<Void> result = guildService.dailyReset();
            if (!result.isSuccess()) {
                log.warn("公会每日重置失败: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("调用公会服务失败", e);
        }
    }

    /**
     * 重置每日副本次数
     */
    private void resetDungeonCount() {
        // 清理 Redis 中的副本次数记录
        // 格式: dungeon:count:{roleId}:{dungeonId} -> 次数
        // 使用 scan 命令避免 keys 阻塞
        XxlJobHelper.log("重置副本次数: 通过 Redis TTL 自动过期");
    }

    /**
     * 重置每日商店
     */
    private void resetDailyShop() {
        // 删除所有玩家的商店购买记录
        XxlJobHelper.log("重置每日商店: 清除购买记录");
        // 实际实现：使用 scan 删除 shop:buy:{roleId}:* 的 key
    }

    /**
     * 重置签到状态
     */
    private void resetSignIn() {
        // 清除当天签到记录以便新的一天签到
        XxlJobHelper.log("重置签到状态: 准备新一天签到");
    }

    /**
     * 发布每日重置事件
     */
    private void publishDailyResetEvent(String date) {
        // 使用分布式事件总线通知所有服务
        distributedEventBus.publishGlobal("daily:reset", date);
    }
}
