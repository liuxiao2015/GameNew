package com.game.service.game.listener;

import com.game.api.common.ProtocolConstants;
import com.game.core.event.EventListener;
import com.game.core.push.PushService;
import com.game.core.rank.RankService;
import com.game.proto.RewardInfo;
import com.game.proto.S2C_LevelUp;
import com.game.proto.S2C_PlayerAttrChange;
import com.game.service.game.event.PlayerEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏事件监听器
 * <p>
 * 监听游戏内部事件，执行相关业务逻辑：
 * <ul>
 *     <li>玩家升级 -> 发放奖励、推送客户端、更新排行榜</li>
 *     <li>战力变化 -> 更新排行榜</li>
 *     <li>登录/登出 -> 记录日志、统计在线</li>
 * </ul>
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - EventListener: 声明式事件监听
 * - PushService: 推送消息给客户端
 * - RankService: 更新排行榜
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final PushService pushService;
    private final RankService rankService;

    /**
     * 推送协议号
     */
    private static final int PUSH_LEVEL_UP = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x03;
    private static final int PUSH_ATTR_CHANGE = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x02;

    // ==================== 玩家升级 ====================

    /**
     * 处理玩家升级事件
     * <p>
     * 优先级 100：确保在其他监听器之前执行（用于发放基础奖励）
     * </p>
     */
    @EventListener(priority = 100)
    public void onPlayerLevelUp(PlayerEvents.PlayerLevelUpEvent event) {
        long roleId = event.getRoleId();
        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();

        log.info("玩家升级: roleId={}, {} -> {}", roleId, oldLevel, newLevel);

        // 1. 更新等级排行榜
        rankService.updateScore(RankService.RANK_LEVEL, roleId, newLevel);

        // 2. 计算升级奖励
        RewardInfo reward = calculateLevelUpReward(oldLevel, newLevel);

        // 3. 推送升级消息给客户端
        S2C_LevelUp pushMessage = S2C_LevelUp.newBuilder()
                .setOldLevel(oldLevel)
                .setNewLevel(newLevel)
                .setReward(reward)
                .build();
        pushService.push(roleId, PUSH_LEVEL_UP, pushMessage);

        // 4. TODO: 发放奖励 (通过 PlayerActor)
    }

    /**
     * 计算升级奖励
     */
    private RewardInfo calculateLevelUpReward(int oldLevel, int newLevel) {
        // 简化计算：每升一级奖励 100 金币和 50 经验
        long gold = (long) (newLevel - oldLevel) * 100;
        long exp = (long) (newLevel - oldLevel) * 50;

        return RewardInfo.newBuilder()
                .setGold(gold)
                .setExp(exp)
                .build();
    }

    // ==================== 战力变化 ====================

    /**
     * 处理战力变化事件
     */
    @EventListener
    public void onCombatPowerChange(PlayerEvents.CombatPowerChangeEvent event) {
        long roleId = event.getRoleId();
        long oldPower = event.getOldPower();
        long newPower = event.getNewPower();

        log.debug("战力变化: roleId={}, {} -> {}, source={}", 
                roleId, oldPower, newPower, event.getSource());

        // 1. 更新战力排行榜
        rankService.updateScore(RankService.RANK_COMBAT_POWER, roleId, newPower);

        // 2. 推送属性变化给客户端
        S2C_PlayerAttrChange pushMessage = S2C_PlayerAttrChange.newBuilder()
                .setAttrType(1) // 1 = 战力
                .setOldValue(oldPower)
                .setNewValue(newPower)
                .setReason(event.getSource())
                .build();
        pushService.push(roleId, PUSH_ATTR_CHANGE, pushMessage);
    }

    // ==================== 货币变化 ====================

    /**
     * 处理金币变化事件
     */
    @EventListener
    public void onGoldChange(PlayerEvents.GoldChangeEvent event) {
        log.debug("金币变化: roleId={}, {} -> {}, reason={}", 
                event.getRoleId(), event.getOldValue(), event.getNewValue(), event.getReason());

        // 推送属性变化
        S2C_PlayerAttrChange pushMessage = S2C_PlayerAttrChange.newBuilder()
                .setAttrType(2) // 2 = 金币
                .setOldValue(event.getOldValue())
                .setNewValue(event.getNewValue())
                .setReason(event.getReason())
                .build();
        pushService.push(event.getRoleId(), PUSH_ATTR_CHANGE, pushMessage);
    }

    /**
     * 处理钻石变化事件
     */
    @EventListener
    public void onDiamondChange(PlayerEvents.DiamondChangeEvent event) {
        log.debug("钻石变化: roleId={}, {} -> {}, reason={}", 
                event.getRoleId(), event.getOldValue(), event.getNewValue(), event.getReason());

        // 推送属性变化
        S2C_PlayerAttrChange pushMessage = S2C_PlayerAttrChange.newBuilder()
                .setAttrType(3) // 3 = 钻石
                .setOldValue(event.getOldValue())
                .setNewValue(event.getNewValue())
                .setReason(event.getReason())
                .build();
        pushService.push(event.getRoleId(), PUSH_ATTR_CHANGE, pushMessage);
    }

    // ==================== 登录登出 ====================

    /**
     * 处理玩家登录事件
     */
    @EventListener(async = true) // 异步处理，不阻塞登录流程
    public void onPlayerLogin(PlayerEvents.PlayerLoginEvent event) {
        log.info("玩家登录: roleId={}, name={}, level={}, serverId={}", 
                event.getRoleId(), event.getRoleName(), event.getLevel(), event.getServerId());

        // TODO: 
        // 1. 记录登录日志
        // 2. 检查离线收益
        // 3. 发送好友上线通知
    }

    /**
     * 处理玩家登出事件
     */
    @EventListener(async = true)
    public void onPlayerLogout(PlayerEvents.PlayerLogoutEvent event) {
        log.info("玩家登出: roleId={}, name={}, onlineTime={}s", 
                event.getRoleId(), event.getRoleName(), event.getOnlineTime());

        // TODO:
        // 1. 记录登出日志
        // 2. 发送好友离线通知
    }

    // ==================== 公会相关 ====================

    /**
     * 处理玩家加入公会事件
     */
    @EventListener
    public void onPlayerJoinGuild(PlayerEvents.PlayerJoinGuildEvent event) {
        log.info("玩家加入公会: roleId={}, guildId={}, guildName={}", 
                event.getRoleId(), event.getGuildId(), event.getGuildName());

        // TODO: 更新玩家数据中的公会信息
    }

    /**
     * 处理玩家离开公会事件
     */
    @EventListener
    public void onPlayerLeaveGuild(PlayerEvents.PlayerLeaveGuildEvent event) {
        log.info("玩家离开公会: roleId={}, guildId={}, guildName={}, reason={}", 
                event.getRoleId(), event.getGuildId(), event.getGuildName(), event.getReason());

        // TODO: 清除玩家数据中的公会信息
    }
}
