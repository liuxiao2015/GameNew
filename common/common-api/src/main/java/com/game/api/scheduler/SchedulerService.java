package com.game.api.scheduler;

import com.game.common.result.Result;

import java.util.List;

/**
 * 调度服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-scheduler 模块实现
 * 提供给其他服务调用，用于跨服务的定时任务、延时任务触发
 * </p>
 *
 * @author GameServer
 */
public interface SchedulerService {

    /**
     * 触发玩家每日重置
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> triggerDailyReset(long roleId);

    /**
     * 批量触发玩家每日重置
     *
     * @param roleIds 角色 ID 列表
     * @return 操作结果
     */
    Result<Void> batchDailyReset(List<Long> roleIds);

    /**
     * 触发公会每日重置
     *
     * @param guildId 公会 ID
     * @return 操作结果
     */
    Result<Void> triggerGuildDailyReset(long guildId);

    /**
     * 触发排行榜更新
     *
     * @param rankType 排行类型
     * @return 操作结果
     */
    Result<Void> triggerRankUpdate(String rankType);

    /**
     * 发送系统邮件
     *
     * @param roleId  角色 ID
     * @param title   邮件标题
     * @param content 邮件内容
     * @param items   附件物品 (格式: itemId:count,itemId:count)
     * @return 操作结果
     */
    Result<Void> sendSystemMail(long roleId, String title, String content, String items);

    /**
     * 群发系统邮件
     *
     * @param title   邮件标题
     * @param content 邮件内容
     * @param items   附件物品
     * @return 操作结果
     */
    Result<Void> broadcastSystemMail(String title, String content, String items);
}
