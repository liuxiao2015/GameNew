package com.game.service.game.rpc;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildInternalService;
import com.game.api.guild.GuildService;
import com.game.api.rank.RankEntryDTO;
import com.game.api.rank.RankService;
import com.game.api.scheduler.SchedulerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RPC 服务调用器
 * <p>
 * Game 服务调用其他服务的统一入口
 * 封装了所有跨服务 RPC 调用，提供统一的错误处理和日志记录
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class RpcServiceCaller {

    // ==================== 公会服务 ====================

    /**
     * 公会服务 - 公开接口
     * <p>
     * 一致性哈希: 按 guildId 路由，确保同一公会的请求路由到同一实例
     * </p>
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 5000,
            retries = 0,
            loadbalance = "consistenthash",
            parameters = {"hash.arguments", "0", "hash.nodes", "160"},
            check = false
    )
    private GuildService guildService;

    /**
     * 公会服务 - 内部接口
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 3000,
            retries = 0,
            loadbalance = "consistenthash",
            parameters = {"hash.arguments", "0", "hash.nodes", "160"},
            check = false
    )
    private GuildInternalService guildInternalService;

    // ==================== 排行服务 ====================

    /**
     * 排行服务
     * <p>
     * 轮询负载: 排行服务无状态，使用轮询
     * </p>
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 3000,
            retries = 1,
            loadbalance = "roundrobin",
            check = false
    )
    private RankService rankService;

    // ==================== 调度服务 ====================

    /**
     * 调度服务 - 定时任务、延时任务
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 10000,
            retries = 0,
            loadbalance = "roundrobin",
            check = false
    )
    private SchedulerService schedulerService;

    // ==================== 公会相关调用 ====================

    /**
     * 获取玩家所在公会信息
     */
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        try {
            return guildService.getPlayerGuild(roleId);
        } catch (Exception e) {
            log.error("调用公会服务失败 - getPlayerGuild: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 创建公会
     */
    public Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId) {
        try {
            return guildService.createGuild(roleId, guildName, declaration, iconId);
        } catch (Exception e) {
            log.error("调用公会服务失败 - createGuild: roleId={}, guildName={}", roleId, guildName, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 通知公会玩家上线
     */
    public void notifyGuildPlayerOnline(long roleId, long guildId) {
        if (guildId <= 0) {
            return;
        }
        try {
            guildInternalService.onPlayerOnline(roleId, guildId);
            log.debug("通知公会玩家上线: roleId={}, guildId={}", roleId, guildId);
        } catch (Exception e) {
            log.error("通知公会玩家上线失败: roleId={}, guildId={}", roleId, guildId, e);
        }
    }

    /**
     * 通知公会玩家下线
     */
    public void notifyGuildPlayerOffline(long roleId, long guildId) {
        if (guildId <= 0) {
            return;
        }
        try {
            guildInternalService.onPlayerOffline(roleId, guildId);
            log.debug("通知公会玩家下线: roleId={}, guildId={}", roleId, guildId);
        } catch (Exception e) {
            log.error("通知公会玩家下线失败: roleId={}, guildId={}", roleId, guildId, e);
        }
    }

    /**
     * 更新公会成员战力
     */
    public void updateGuildMemberCombatPower(long roleId, long guildId, long combatPower) {
        if (guildId <= 0) {
            return;
        }
        try {
            guildInternalService.updateMemberCombatPower(roleId, guildId, combatPower);
            log.debug("更新公会成员战力: roleId={}, guildId={}, combatPower={}", roleId, guildId, combatPower);
        } catch (Exception e) {
            log.error("更新公会成员战力失败: roleId={}, guildId={}", roleId, guildId, e);
        }
    }

    // ==================== 排行相关调用 ====================

    /**
     * 更新排行榜分数
     */
    public void updateRankScore(String rankType, long entityId, double score, String extra) {
        try {
            rankService.updateScore(rankType, entityId, score, extra);
            log.debug("更新排行榜分数: rankType={}, entityId={}, score={}", rankType, entityId, score);
        } catch (Exception e) {
            log.error("更新排行榜分数失败: rankType={}, entityId={}", rankType, entityId, e);
        }
    }

    /**
     * 获取排行榜前 N 名
     */
    public Result<List<RankEntryDTO>> getTopRank(String rankType, int count) {
        try {
            return rankService.getTopRank(rankType, count);
        } catch (Exception e) {
            log.error("获取排行榜失败: rankType={}", rankType, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 获取玩家排名
     */
    public Result<Long> getPlayerRank(String rankType, long roleId) {
        try {
            return rankService.getRank(rankType, roleId);
        } catch (Exception e) {
            log.error("获取玩家排名失败: rankType={}, roleId={}", rankType, roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    // ==================== 调度服务相关调用 ====================

    /**
     * 发送系统邮件
     */
    public Result<Void> sendSystemMail(long roleId, String title, String content, String items) {
        try {
            return schedulerService.sendSystemMail(roleId, title, content, items);
        } catch (Exception e) {
            log.error("发送系统邮件失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 群发系统邮件
     */
    public Result<Void> broadcastSystemMail(String title, String content, String items) {
        try {
            return schedulerService.broadcastSystemMail(title, content, items);
        } catch (Exception e) {
            log.error("群发系统邮件失败", e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }
}
