package com.game.service.guild.rpc;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerInternalService;
import com.game.api.player.PlayerService;
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
 * Guild 服务调用其他服务的统一入口
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class RpcServiceCaller {

    // ==================== 玩家服务 ====================

    /**
     * 玩家服务 - 公开接口
     * <p>
     * 一致性哈希: 按 roleId 路由
     * </p>
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
    private PlayerService playerService;

    /**
     * 玩家服务 - 内部接口
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
    private PlayerInternalService playerInternalService;

    // ==================== 排行服务 ====================

    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 3000,
            retries = 1,
            loadbalance = "roundrobin",
            check = false
    )
    private RankService rankService;

    // ==================== 玩家相关调用 ====================

    /**
     * 获取玩家信息
     */
    public Result<PlayerDTO> getPlayerInfo(long roleId) {
        try {
            return playerService.getPlayerInfo(roleId);
        } catch (Exception e) {
            log.error("调用玩家服务失败 - getPlayerInfo: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 批量获取玩家信息
     */
    public Result<List<PlayerDTO>> batchGetPlayers(List<Long> roleIds) {
        try {
            return playerInternalService.batchGetPlayers(roleIds);
        } catch (Exception e) {
            log.error("调用玩家服务失败 - batchGetPlayers: count={}", roleIds.size(), e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 设置玩家公会信息
     * <p>
     * 当玩家加入/退出公会时调用
     * </p>
     */
    public Result<Void> setPlayerGuild(long roleId, long guildId, String guildName, int position) {
        try {
            return playerInternalService.setPlayerGuild(roleId, guildId, guildName, position);
        } catch (Exception e) {
            log.error("设置玩家公会信息失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 检查玩家货币是否足够
     */
    public boolean checkPlayerCurrency(long roleId, int currencyType, long amount) {
        try {
            Result<Boolean> result = playerInternalService.checkCurrency(roleId, currencyType, amount);
            return result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.error("检查玩家货币失败: roleId={}", roleId, e);
            return false;
        }
    }

    /**
     * 扣除玩家货币
     */
    public Result<Void> deductPlayerCurrency(long roleId, int currencyType, long amount, String reason) {
        try {
            return playerInternalService.deductCurrency(roleId, currencyType, amount, reason);
        } catch (Exception e) {
            log.error("扣除玩家货币失败: roleId={}, type={}, amount={}", roleId, currencyType, amount, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 获取玩家战力
     */
    public long getPlayerCombatPower(long roleId) {
        try {
            Result<Long> result = playerInternalService.getPlayerCombatPower(roleId);
            return result.isSuccess() ? result.getData() : 0L;
        } catch (Exception e) {
            log.error("获取玩家战力失败: roleId={}", roleId, e);
            return 0L;
        }
    }

    /**
     * 检查玩家是否在线
     */
    public boolean isPlayerOnline(long roleId) {
        try {
            Result<Boolean> result = playerInternalService.isOnline(roleId);
            return result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.error("检查玩家在线状态失败: roleId={}", roleId, e);
            return false;
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

    // ==================== 排行相关调用 ====================

    /**
     * 更新公会排行榜分数
     */
    public void updateGuildRankScore(long guildId, double score, String guildName) {
        try {
            rankService.updateScore("guild", guildId, score, guildName);
            log.debug("更新公会排行: guildId={}, score={}", guildId, score);
        } catch (Exception e) {
            log.error("更新公会排行失败: guildId={}", guildId, e);
        }
    }
}
