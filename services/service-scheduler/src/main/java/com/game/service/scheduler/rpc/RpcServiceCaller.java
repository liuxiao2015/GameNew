package com.game.service.scheduler.rpc;

import com.game.api.guild.GuildInternalService;
import com.game.api.player.PlayerInternalService;
import com.game.api.rank.RankService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RPC 服务调用器
 * <p>
 * Scheduler 服务调用其他服务的统一入口
 * 定时任务需要调用多个服务执行业务逻辑
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class RpcServiceCaller {

    // ==================== 玩家服务 ====================

    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 5000,
            retries = 0,
            loadbalance = "consistenthash",
            parameters = {"hash.arguments", "0", "hash.nodes", "160"},
            check = false
    )
    private PlayerInternalService playerInternalService;

    // ==================== 公会服务 ====================

    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 5000,
            retries = 0,
            loadbalance = "consistenthash",
            parameters = {"hash.arguments", "0", "hash.nodes", "160"},
            check = false
    )
    private GuildInternalService guildInternalService;

    // ==================== 排行服务 ====================

    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 10000,
            retries = 1,
            loadbalance = "roundrobin",
            check = false
    )
    private RankService rankService;

    // ==================== 玩家相关调用 ====================

    /**
     * 触发玩家每日重置
     */
    public Result<Void> triggerPlayerDailyReset(long roleId) {
        try {
            return playerInternalService.dailyReset(roleId);
        } catch (Exception e) {
            log.error("触发玩家每日重置失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 触发玩家每周重置
     */
    public Result<Void> triggerPlayerWeeklyReset(long roleId) {
        try {
            return playerInternalService.weeklyReset(roleId);
        } catch (Exception e) {
            log.error("触发玩家每周重置失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 获取在线玩家列表
     */
    public List<Long> getOnlinePlayers() {
        try {
            Result<List<Long>> result = playerInternalService.getOnlinePlayers();
            return result.isSuccess() ? result.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取在线玩家列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 发送邮件给玩家
     */
    public Result<Void> sendMailToPlayer(long roleId, String title, String content, Map<Integer, Long> attachments) {
        try {
            return playerInternalService.sendMail(roleId, title, content, attachments);
        } catch (Exception e) {
            log.error("发送邮件失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    // ==================== 公会相关调用 ====================

    /**
     * 触发公会每日重置
     */
    public Result<Void> triggerGuildDailyReset(long guildId) {
        try {
            return guildInternalService.dailyReset(guildId);
        } catch (Exception e) {
            log.error("触发公会每日重置失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 触发公会每周重置
     */
    public Result<Void> triggerGuildWeeklyReset(long guildId) {
        try {
            return guildInternalService.weeklyReset(guildId);
        } catch (Exception e) {
            log.error("触发公会每周重置失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 获取所有公会 ID
     */
    public List<Long> getAllGuildIds() {
        try {
            Result<List<Long>> result = guildInternalService.getAllGuildIds();
            return result.isSuccess() ? result.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取公会列表失败", e);
            return Collections.emptyList();
        }
    }

    // ==================== 排行相关调用 ====================

    /**
     * 触发排行榜刷新
     */
    public Result<Void> refreshRank(String rankType) {
        try {
            return rankService.refreshRank(rankType);
        } catch (Exception e) {
            log.error("刷新排行榜失败: rankType={}", rankType, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }
}
