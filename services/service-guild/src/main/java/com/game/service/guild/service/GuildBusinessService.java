package com.game.service.guild.service;

import com.game.actor.core.ActorSystem;
import com.game.api.common.ProtocolConstants;
import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.core.event.EventBus;
import com.game.core.id.IdService;
import com.game.core.lock.LockService;
import com.game.core.push.PushService;
import com.game.core.rank.RankService;
import com.game.data.mongo.MongoService;
import com.game.entity.document.GuildData;
import com.game.proto.*;
import com.game.service.guild.actor.GuildActor;
import com.game.service.guild.event.GuildEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公会业务服务
 * <p>
 * 提供公会相关的业务逻辑，演示框架各项能力的使用：
 * <ul>
 *     <li>Actor: 通过 ActorSystem 管理公会 Actor</li>
 *     <li>RPC: 调用玩家服务获取玩家信息</li>
 *     <li>EventBus: 发布公会相关事件</li>
 *     <li>PushService: 推送消息给公会成员</li>
 *     <li>RankService: 更新公会排行榜</li>
 *     <li>LockService: 分布式锁保证创建公会的唯一性</li>
 *     <li>IdService: 生成公会唯一 ID</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuildBusinessService {

    @Qualifier("guildActorSystem")
    private final ActorSystem<GuildActor> guildActorSystem;
    private final MongoService mongoService;
    private final EventBus eventBus;
    private final PushService pushService;
    private final RankService rankService;
    private final LockService lockService;
    private final IdService idService;

    /**
     * 玩家服务 (跨服务调用)
     */
    @DubboReference(check = false)
    private PlayerService playerService;

    /**
     * 推送协议号
     */
    private static final int PUSH_GUILD_MEMBER_CHANGE = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x20;
    private static final int PUSH_GUILD_INFO_CHANGE = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x21;
    private static final int PUSH_GUILD_APPLY = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x22;

    // ==================== 公会创建 ====================

    /**
     * 创建公会
     * <p>
     * 使用分布式锁确保公会名称唯一性
     * </p>
     */
    public Result<GuildInfo> createGuild(long roleId, String guildName, String declaration, int iconId) {
        // 1. 获取玩家信息 (跨服务调用)
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (!playerResult.isSuccess()) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        PlayerDTO player = playerResult.getData();

        // 2. 检查玩家是否已有公会
        if (player.getGuildId() > 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "已有公会");
        }

        // 3. 使用分布式锁检查公会名称唯一性
        String lockKey = "guild:create:" + guildName;
        return lockService.executeWithLock(lockKey, () -> {
            // 4. 生成公会 ID
            long guildId = idService.nextId();

            // 5. 创建公会数据
            GuildData guildData = new GuildData();
            guildData.setId(String.valueOf(guildId));
            guildData.setGuildId(guildId);
            guildData.setGuildName(guildName);
            guildData.setDeclaration(declaration);
            guildData.setIconId(iconId);
            guildData.setLeaderId(roleId);
            guildData.setLeaderName(player.getRoleName());
            guildData.setLevel(1);
            guildData.setMaxMember(50);
            guildData.setGuildCreateTime(System.currentTimeMillis());

            // 添加创建者为会长
            GuildData.GuildMember leader = new GuildData.GuildMember();
            leader.setRoleId(roleId);
            leader.setRoleName(player.getRoleName());
            leader.setLevel(player.getLevel());
            leader.setAvatarId(player.getAvatarId());
            leader.setPosition(3); // 会长
            leader.setJoinTime(System.currentTimeMillis());
            leader.setCombatPower(player.getCombatPower());
            guildData.getMembers().add(leader);
            guildData.setMemberCount(1);
            guildData.setTotalCombatPower(player.getCombatPower());

            // 6. 保存到数据库
            mongoService.save(guildData);

            // 7. 更新玩家公会信息 (跨服务调用)
            playerService.setGuildInfo(roleId, guildId, guildName, 3);

            // 8. 更新公会排行榜
            rankService.updateScore(RankService.RANK_GUILD, guildId, player.getCombatPower());

            // 9. 发布公会创建事件
            eventBus.publish(new GuildEvents.GuildCreatedEvent(
                    guildId, guildName, roleId, player.getRoleName()));

            log.info("创建公会成功: guildId={}, guildName={}, leaderId={}", guildId, guildName, roleId);

            return Result.success(buildGuildInfo(guildData));
        });
    }

    // ==================== 加入公会 ====================

    /**
     * 申请加入公会
     */
    public Result<Void> applyJoinGuild(long roleId, long guildId, String message) {
        // 1. 获取玩家信息
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (!playerResult.isSuccess()) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        PlayerDTO player = playerResult.getData();

        if (player.getGuildId() > 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "已有公会");
        }

        // 2. 获取公会 Actor
        GuildActor guildActor = guildActorSystem.getActor(guildId);
        if (guildActor == null || guildActor.getData() == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        GuildData guildData = guildActor.getData();

        // 3. 检查公会人数
        if (guildData.getMemberCount() >= guildData.getMaxMember()) {
            return Result.fail(ErrorCode.GUILD_FULL);
        }

        // 4. 根据加入类型处理
        if (guildData.getJoinType() == 0) {
            // 自由加入 - 直接加入
            return doJoinGuild(roleId, player, guildData);
        } else {
            // 需要审批或禁止加入
            return Result.fail(ErrorCode.PARAM_ERROR, "需要审批");
        }
    }

    /**
     * 执行加入公会
     */
    private Result<Void> doJoinGuild(long roleId, PlayerDTO player, GuildData guildData) {
        // 更新玩家公会信息
        playerService.setGuildInfo(roleId, guildData.getGuildId(), guildData.getGuildName(), 0);

        // 更新排行榜
        rankService.updateScore(RankService.RANK_GUILD, guildData.getGuildId(), 
                guildData.getTotalCombatPower());

        // 发布事件
        eventBus.publish(new GuildEvents.MemberJoinedEvent(
                guildData.getGuildId(), guildData.getGuildName(), roleId, player.getRoleName()));

        log.info("玩家加入公会: roleId={}, guildId={}", roleId, guildData.getGuildId());
        return Result.success();
    }

    // ==================== 退出公会 ====================

    /**
     * 退出公会
     */
    public Result<Void> leaveGuild(long roleId) {
        // 获取玩家信息
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (!playerResult.isSuccess()) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }
        PlayerDTO player = playerResult.getData();

        long guildId = player.getGuildId();
        if (guildId <= 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "不在公会中");
        }

        // 获取公会 Actor
        GuildActor guildActor = guildActorSystem.getActor(guildId);
        if (guildActor == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        GuildData guildData = guildActor.getData();

        // 会长不能直接退出
        if (guildData.getLeaderId() == roleId) {
            return Result.fail(ErrorCode.PARAM_ERROR, "会长不能直接退出");
        }

        // 更新玩家公会信息
        playerService.setGuildInfo(roleId, 0, null, 0);

        // 发布事件
        eventBus.publish(new GuildEvents.MemberLeftEvent(
                guildId, guildData.getGuildName(), roleId, 
                GuildEvents.MemberLeftEvent.LeaveType.LEAVE));

        log.info("玩家退出公会: roleId={}, guildId={}", roleId, guildId);
        return Result.success();
    }

    // ==================== 公会捐献 ====================

    /**
     * 公会捐献
     */
    public Result<GuildDonateResult> donate(long roleId, int donateType, long amount) {
        // 获取玩家公会
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (!playerResult.isSuccess()) {
            return Result.fail(ErrorCode.ROLE_NOT_FOUND);
        }

        long guildId = playerResult.getData().getGuildId();
        if (guildId <= 0) {
            return Result.fail(ErrorCode.PARAM_ERROR, "不在公会中");
        }

        // 扣除货币 (跨服务调用)
        if (donateType == 1) {
            Result<Void> costResult = playerService.addGold(roleId, -amount, "公会捐献");
            if (!costResult.isSuccess()) {
                return Result.fail(ErrorCode.GOLD_NOT_ENOUGH);
            }
        } else if (donateType == 2) {
            Result<Void> costResult = playerService.addDiamond(roleId, -amount, "公会捐献");
            if (!costResult.isSuccess()) {
                return Result.fail(ErrorCode.DIAMOND_NOT_ENOUGH);
            }
        }

        // 发布捐献事件
        eventBus.publish(new GuildEvents.GuildDonateEvent(guildId, roleId, donateType, amount, amount));

        long contribution = amount;
        long guildExp = amount / 10;

        log.info("公会捐献: roleId={}, guildId={}, type={}, amount={}", 
                roleId, guildId, donateType, amount);

        return Result.success(new GuildDonateResult(contribution, guildExp));
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建公会信息
     */
    private GuildInfo buildGuildInfo(GuildData data) {
        return GuildInfo.newBuilder()
                .setGuildId(data.getGuildId())
                .setGuildName(data.getGuildName())
                .setLevel(data.getLevel())
                .setExp(data.getExp())
                .setIconId(data.getIconId())
                .setDeclaration(data.getDeclaration() != null ? data.getDeclaration() : "")
                .setLeaderId(data.getLeaderId())
                .setLeaderName(data.getLeaderName())
                .setMemberCount(data.getMemberCount())
                .setMaxMember(data.getMaxMember())
                .setCreateTime(data.getGuildCreateTime())
                .setJoinType(data.getJoinType())
                .setJoinLevel(data.getJoinLevel())
                .build();
    }

    // ==================== 数据类 ====================

    public record GuildDonateResult(long contribution, long guildExp) {}
}
