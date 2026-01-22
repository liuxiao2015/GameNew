package com.game.service.scheduler.service;

import com.game.api.scheduler.SchedulerService;
import com.game.common.result.Result;
import com.game.service.scheduler.rpc.RpcServiceCaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    private final RpcServiceCaller rpcCaller;

    @Override
    public Result<Void> triggerDailyReset(long roleId) {
        log.info("触发玩家每日重置: roleId={}", roleId);
        return rpcCaller.triggerPlayerDailyReset(roleId);
    }

    @Override
    public Result<Void> batchDailyReset(List<Long> roleIds) {
        log.info("批量触发玩家每日重置: count={}", roleIds.size());
        int successCount = 0;
        int failCount = 0;

        for (Long roleId : roleIds) {
            Result<Void> result = rpcCaller.triggerPlayerDailyReset(roleId);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("批量每日重置完成: success={}, fail={}", successCount, failCount);
        return Result.success();
    }

    @Override
    public Result<Void> triggerGuildDailyReset(long guildId) {
        log.info("触发公会每日重置: guildId={}", guildId);
        return rpcCaller.triggerGuildDailyReset(guildId);
    }

    @Override
    public Result<Void> triggerRankUpdate(String rankType) {
        log.info("触发排行榜更新: rankType={}", rankType);
        return rpcCaller.refreshRank(rankType);
    }

    @Override
    public Result<Void> sendSystemMail(long roleId, String title, String content, String items) {
        log.info("发送系统邮件: roleId={}, title={}", roleId, title);
        Map<Integer, Long> attachments = parseItems(items);
        return rpcCaller.sendMailToPlayer(roleId, title, content, attachments);
    }

    @Override
    public Result<Void> broadcastSystemMail(String title, String content, String items) {
        log.info("群发系统邮件: title={}", title);
        Map<Integer, Long> attachments = parseItems(items);

        // 获取所有在线玩家
        List<Long> onlinePlayers = rpcCaller.getOnlinePlayers();
        int sentCount = 0;

        for (Long roleId : onlinePlayers) {
            Result<Void> result = rpcCaller.sendMailToPlayer(roleId, title, content, attachments);
            if (result.isSuccess()) {
                sentCount++;
            }
        }

        log.info("群发邮件完成: sentCount={}", sentCount);
        return Result.success();
    }

    /**
     * 解析物品字符串
     *
     * @param items 格式: itemId:count,itemId:count
     * @return 物品映射
     */
    private Map<Integer, Long> parseItems(String items) {
        Map<Integer, Long> result = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return result;
        }

        String[] pairs = items.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                try {
                    int itemId = Integer.parseInt(parts[0].trim());
                    long count = Long.parseLong(parts[1].trim());
                    result.put(itemId, count);
                } catch (NumberFormatException e) {
                    log.warn("解析物品失败: {}", pair);
                }
            }
        }
        return result;
    }
}
