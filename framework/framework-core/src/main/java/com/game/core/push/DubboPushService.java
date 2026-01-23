package com.game.core.push;

import com.game.api.broadcast.BroadcastService;
import com.game.api.push.PushTargetService;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 Dubbo 的推送服务
 * <p>
 * 使用 Dubbo RPC 替代 Redis Pub/Sub 实现消息推送，优势：
 * <ul>
 *     <li>减少 Redis 压力</li>
 *     <li>类型安全（接口定义）</li>
 *     <li>可靠性更高（有返回值确认）</li>
 *     <li>内置超时和重试</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Autowired
 * private DubboPushService pushService;
 *
 * // 推送给单个玩家
 * pushService.pushToPlayer(roleId, MethodId.Push.ITEM_CHANGE, itemChangeMsg);
 *
 * // 推送给公会成员
 * pushService.pushToGuild(guildId, MethodId.Push.GUILD_NOTICE, noticeMsg);
 *
 * // 全服广播
 * pushService.broadcast(MethodId.Push.SYSTEM_NOTICE, systemNoticeMsg);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class DubboPushService {

    /**
     * 推送服务 - 一致性哈希路由（按 roleId 路由到对应 Gateway）
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            timeout = 3000,
            retries = 0,
            loadbalance = "consistenthash",
            parameters = {"hash.arguments", "0"},
            check = false
    )
    private PushTargetService pushTargetService;

    /**
     * 推送服务 - 广播模式（发送到所有 Gateway）
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            cluster = "broadcast",
            timeout = 5000,
            check = false
    )
    private PushTargetService broadcastPushService;

    /**
     * 广播服务（发送到所有业务服务实例）
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            cluster = "broadcast",
            timeout = 3000,
            check = false
    )
    private BroadcastService broadcastService;

    // ==================== 玩家推送 ====================

    /**
     * 推送消息到指定玩家
     *
     * @param roleId     角色 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return true 如果推送成功
     */
    public boolean pushToPlayer(long roleId, int protocolId, Message message) {
        try {
            return pushTargetService.pushToPlayer(roleId, protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("推送消息失败: roleId={}, protocolId={}", roleId, protocolId, e);
            return false;
        }
    }

    /**
     * 推送消息到指定玩家（原始字节）
     */
    public boolean pushToPlayer(long roleId, int protocolId, byte[] data) {
        try {
            return pushTargetService.pushToPlayer(roleId, protocolId, data);
        } catch (Exception e) {
            log.error("推送消息失败: roleId={}, protocolId={}", roleId, protocolId, e);
            return false;
        }
    }

    /**
     * 推送消息到多个玩家
     *
     * @param roleIds    角色 ID 列表
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToPlayers(List<Long> roleIds, int protocolId, Message message) {
        if (roleIds == null || roleIds.isEmpty()) {
            return 0;
        }

        byte[] data = message.toByteArray();
        int successCount = 0;

        // 按玩家逐个推送（会路由到不同 Gateway）
        for (Long roleId : roleIds) {
            if (pushToPlayer(roleId, protocolId, data)) {
                successCount++;
            }
        }

        return successCount;
    }

    // ==================== 公会推送 ====================

    /**
     * 推送消息到公会成员
     *
     * @param guildId    公会 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToGuild(long guildId, int protocolId, Message message) {
        try {
            // 使用广播模式，让所有 Gateway 检查自己的公会成员
            return broadcastPushService.pushToGuild(guildId, protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("公会推送失败: guildId={}, protocolId={}", guildId, protocolId, e);
            return 0;
        }
    }

    // ==================== 全服广播 ====================

    /**
     * 广播消息到所有在线玩家
     *
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的总数量
     */
    public int broadcast(int protocolId, Message message) {
        try {
            // 广播模式会发送到所有 Gateway 实例
            return broadcastPushService.broadcast(protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("全服广播失败: protocolId={}", protocolId, e);
            return 0;
        }
    }

    /**
     * 广播消息到指定服务器的在线玩家
     *
     * @param serverId   服务器 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int broadcastToServer(int serverId, int protocolId, Message message) {
        try {
            return broadcastPushService.broadcastToServer(serverId, protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("服务器广播失败: serverId={}, protocolId={}", serverId, protocolId, e);
            return 0;
        }
    }

    // ==================== 公会广播 ====================

    /**
     * 推送消息到公会在线成员（排除指定玩家）
     *
     * @param guildId       公会 ID
     * @param excludeRoleId 排除的角色 ID（通常是消息发送者）
     * @param protocolId    协议号
     * @param message       Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToGuildExclude(long guildId, long excludeRoleId, int protocolId, Message message) {
        try {
            return broadcastPushService.pushToGuildExclude(guildId, protocolId, 
                    message.toByteArray(), excludeRoleId);
        } catch (Exception e) {
            log.error("公会推送失败: guildId={}, protocolId={}", guildId, protocolId, e);
            return 0;
        }
    }

    // ==================== 条件广播 ====================

    /**
     * 条件广播 - 推送到满足条件的玩家
     *
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @param conditions 过滤条件
     * @return 成功推送的数量
     */
    public int broadcastWithFilter(int protocolId, Message message, java.util.Map<String, Object> conditions) {
        try {
            return broadcastPushService.broadcastWithFilter(protocolId, message.toByteArray(), conditions);
        } catch (Exception e) {
            log.error("条件广播失败: protocolId={}, conditions={}", protocolId, conditions, e);
            return 0;
        }
    }

    /**
     * 推送消息到指定等级以上的玩家
     *
     * @param minLevel   最低等级
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int broadcastToLevel(int minLevel, int protocolId, Message message) {
        return broadcastWithFilter(protocolId, message, java.util.Map.of("minLevel", minLevel));
    }

    /**
     * 推送消息到 VIP 玩家
     *
     * @param minVipLevel 最低 VIP 等级
     * @param protocolId  协议号
     * @param message     Protobuf 消息
     * @return 成功推送的数量
     */
    public int broadcastToVip(int minVipLevel, int protocolId, Message message) {
        return broadcastWithFilter(protocolId, message, java.util.Map.of("vipLevel", minVipLevel));
    }

    // ==================== 场景/房间广播 ====================

    /**
     * 推送消息到场景内玩家
     *
     * @param sceneId    场景 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToScene(long sceneId, int protocolId, Message message) {
        try {
            return broadcastPushService.pushToScene(sceneId, protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("场景推送失败: sceneId={}, protocolId={}", sceneId, protocolId, e);
            return 0;
        }
    }

    /**
     * 推送消息到房间内玩家
     *
     * @param roomId     房间 ID
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToRoom(long roomId, int protocolId, Message message) {
        try {
            return broadcastPushService.pushToRoom(roomId, protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("房间推送失败: roomId={}, protocolId={}", roomId, protocolId, e);
            return 0;
        }
    }

    /**
     * 推送消息到附近玩家（AOI）
     *
     * @param sceneId    场景 ID
     * @param centerX    中心 X 坐标
     * @param centerY    中心 Y 坐标
     * @param radius     半径
     * @param protocolId 协议号
     * @param message    Protobuf 消息
     * @return 成功推送的数量
     */
    public int pushToNearby(long sceneId, int centerX, int centerY, int radius,
                            int protocolId, Message message) {
        try {
            return broadcastPushService.pushToNearby(sceneId, centerX, centerY, radius,
                    protocolId, message.toByteArray());
        } catch (Exception e) {
            log.error("AOI推送失败: sceneId={}, protocolId={}", sceneId, protocolId, e);
            return 0;
        }
    }

    // ==================== 服务间广播 ====================

    /**
     * 广播配置热更新到所有服务
     *
     * @param configName 配置名称
     */
    public void broadcastConfigReload(String configName) {
        try {
            broadcastService.onConfigReload(configName, System.currentTimeMillis());
            log.info("广播配置刷新: configName={}", configName);
        } catch (Exception e) {
            log.error("广播配置刷新失败: configName={}", configName, e);
        }
    }

    /**
     * 广播缓存失效到所有服务
     *
     * @param cacheName 缓存名称
     * @param key       缓存 Key（null 表示清空整个缓存）
     */
    public void broadcastCacheEvict(String cacheName, String key) {
        try {
            broadcastService.onCacheEvict(cacheName, key);
            log.info("广播缓存失效: cacheName={}, key={}", cacheName, key);
        } catch (Exception e) {
            log.error("广播缓存失效失败: cacheName={}, key={}", cacheName, key, e);
        }
    }

    /**
     * 广播服务器公告
     *
     * @param noticeType 公告类型
     * @param content    公告内容
     * @param duration   显示时长（秒）
     */
    public void broadcastServerNotice(int noticeType, String content, int duration) {
        try {
            broadcastService.onServerNotice(noticeType, content, duration);
            log.info("广播服务器公告: type={}, content={}", noticeType, content);
        } catch (Exception e) {
            log.error("广播服务器公告失败: type={}", noticeType, e);
        }
    }

    /**
     * 广播活动状态变更
     *
     * @param activityId 活动 ID
     * @param status     状态
     */
    public void broadcastActivityChange(int activityId, int status) {
        try {
            broadcastService.onActivityChange(activityId, status);
            log.info("广播活动变更: activityId={}, status={}", activityId, status);
        } catch (Exception e) {
            log.error("广播活动变更失败: activityId={}", activityId, e);
        }
    }

    /**
     * 广播踢人到所有 Gateway
     *
     * @param roleId 角色 ID
     * @param reason 踢出原因
     */
    public void broadcastKickPlayer(long roleId, String reason) {
        try {
            broadcastService.onKickPlayer(roleId, reason);
            log.info("广播踢人: roleId={}, reason={}", roleId, reason);
        } catch (Exception e) {
            log.error("广播踢人失败: roleId={}", roleId, e);
        }
    }
}
