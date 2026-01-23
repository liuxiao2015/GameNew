package com.game.service.scheduler.job;

import com.game.api.common.ProtocolConstants;
import com.game.core.event.DistributedEventBus;
import com.game.core.push.PushService;
import com.game.data.redis.RedisService;
import com.game.proto.S2C_SystemNotice;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 活动定时任务
 * <p>
 * 演示活动相关的定时任务：
 * <ul>
 *     <li>活动开始/结束检测</li>
 *     <li>活动公告推送</li>
 *     <li>活动奖励发放</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityJob {

    private final RedisService redisService;
    private final DistributedEventBus distributedEventBus;
    private final PushService pushService;

    /**
     * 推送协议号
     */
    private static final int PUSH_SYSTEM_NOTICE = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x11;

    /**
     * 活动状态检测 - 每分钟执行
     * XXL-Job 配置: 0 * * * * ?
     */
    @XxlJob("activityCheckHandler")
    public void activityCheck() {
        long now = System.currentTimeMillis();
        
        try {
            // 检查待开始的活动
            checkPendingActivities(now);

            // 检查待结束的活动
            checkEndingActivities(now);

        } catch (Exception e) {
            XxlJobHelper.log("活动状态检测异常: {}", e.getMessage());
            log.error("活动状态检测异常", e);
        }
    }

    /**
     * 活动预告 - 活动开始前 30 分钟发送公告
     * XXL-Job 配置: 动态配置
     */
    @XxlJob("activityNoticeHandler")
    public void activityNotice() {
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("发送活动预告: {}", param);

        try {
            // 解析参数: activityId,activityName,minutesBefore
            String[] parts = param.split(",");
            if (parts.length < 2) {
                return;
            }

            String activityName = parts[1];
            int minutesBefore = parts.length > 2 ? Integer.parseInt(parts[2]) : 30;

            // 广播活动预告
            String content = String.format("【活动预告】%s 将在 %d 分钟后开始，请做好准备！", 
                    activityName, minutesBefore);
            broadcastNotice("活动预告", content, 1); // 1 = 滚动公告

            log.info("发送活动预告: {}", activityName);

        } catch (Exception e) {
            XxlJobHelper.log("活动预告发送异常: {}", e.getMessage());
            log.error("活动预告发送异常", e);
        }
    }

    /**
     * 活动开始处理
     * XXL-Job 配置: 动态配置
     */
    @XxlJob("activityStartHandler")
    public void activityStart() {
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("活动开始: {}", param);

        try {
            // 解析参数: activityId,activityName
            String[] parts = param.split(",");
            if (parts.length < 2) {
                return;
            }

            int activityId = Integer.parseInt(parts[0]);
            String activityName = parts[1];

            // 1. 更新活动状态为进行中
            redisService.setString("activity:status:" + activityId, "running");

            // 2. 发布活动开始事件
            distributedEventBus.publishGlobal("activity:start", String.valueOf(activityId));

            // 3. 广播活动开始公告
            String content = String.format("【活动开始】%s 已经开始！快来参与吧！", activityName);
            broadcastNotice("活动开始", content, 2); // 2 = 弹窗公告

            log.info("活动开始: activityId={}, name={}", activityId, activityName);

        } catch (Exception e) {
            XxlJobHelper.log("活动开始处理异常: {}", e.getMessage());
            log.error("活动开始处理异常", e);
        }
    }

    /**
     * 活动结束处理
     * XXL-Job 配置: 动态配置
     */
    @XxlJob("activityEndHandler")
    public void activityEnd() {
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("活动结束: {}", param);

        try {
            // 解析参数: activityId,activityName
            String[] parts = param.split(",");
            if (parts.length < 2) {
                return;
            }

            int activityId = Integer.parseInt(parts[0]);
            String activityName = parts[1];

            // 1. 更新活动状态为已结束
            redisService.setString("activity:status:" + activityId, "ended");

            // 2. 发放活动奖励
            sendActivityRewards(activityId);

            // 3. 发布活动结束事件
            distributedEventBus.publishGlobal("activity:end", String.valueOf(activityId));

            // 4. 广播活动结束公告
            String content = String.format("【活动结束】%s 已经结束，奖励已通过邮件发放！", activityName);
            broadcastNotice("活动结束", content, 1);

            log.info("活动结束: activityId={}, name={}", activityId, activityName);

        } catch (Exception e) {
            XxlJobHelper.log("活动结束处理异常: {}", e.getMessage());
            log.error("活动结束处理异常", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查待开始的活动
     */
    private void checkPendingActivities(long now) {
        // 实际实现：从数据库或配置中心读取活动列表
        // 检查 startTime <= now && status == "pending"
        // 然后触发活动开始逻辑
    }

    /**
     * 检查待结束的活动
     */
    private void checkEndingActivities(long now) {
        // 实际实现：从数据库或配置中心读取活动列表
        // 检查 endTime <= now && status == "running"
        // 然后触发活动结束逻辑
    }

    /**
     * 发放活动奖励
     */
    private void sendActivityRewards(int activityId) {
        XxlJobHelper.log("发放活动奖励: activityId={}", activityId);
        // 实际实现：
        // 1. 查询活动参与记录
        // 2. 计算奖励
        // 3. 通过邮件服务发送奖励
    }

    /**
     * 广播系统公告
     */
    private void broadcastNotice(String title, String content, int noticeType) {
        S2C_SystemNotice notice = S2C_SystemNotice.newBuilder()
                .setNoticeType(noticeType)
                .setTitle(title)
                .setContent(content)
                .setStartTime(System.currentTimeMillis())
                .setEndTime(System.currentTimeMillis() + 3600_000) // 1 小时后过期
                .build();

        pushService.broadcast(PUSH_SYSTEM_NOTICE, notice);
    }
}
