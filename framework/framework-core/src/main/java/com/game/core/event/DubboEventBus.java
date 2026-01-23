package com.game.core.event;

import com.game.api.event.DistributedEventService;
import com.game.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 基于 Dubbo 的分布式事件总线
 * <p>
 * 完全替代 Redis Pub/Sub，使用 Dubbo broadcast 模式：
 * <ul>
 *     <li>减少 Redis 压力</li>
 *     <li>有返回值确认（虽然 broadcast 模式可能忽略）</li>
 *     <li>更好的类型安全</li>
 *     <li>利用 Dubbo 的超时和重试机制</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Autowired
 * private DubboEventBus eventBus;
 *
 * // 广播配置刷新
 * eventBus.broadcastConfigReload("item.json");
 *
 * // 广播自定义事件
 * eventBus.broadcast("PlayerLevelUp", JsonUtil.toJson(event));
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboEventBus {

    /**
     * 分布式事件服务（广播模式）
     */
    @DubboReference(
            version = "1.0.0",
            group = "GAME_SERVER",
            cluster = "broadcast",
            timeout = 3000,
            check = false
    )
    private DistributedEventService distributedEventService;

    /**
     * 本地事件总线
     */
    private final EventBus localEventBus;

    // ==================== 配置事件 ====================

    /**
     * 广播配置刷新事件
     */
    public void broadcastConfigReload(String configName) {
        try {
            distributedEventService.publishConfigReload(configName, System.currentTimeMillis());
            log.info("广播配置刷新: configName={}", configName);
        } catch (Exception e) {
            log.error("广播配置刷新失败: configName={}", configName, e);
        }
    }

    /**
     * 广播缓存失效事件
     */
    public void broadcastCacheEvict(String cacheName, String key) {
        try {
            distributedEventService.publishCacheEvict(cacheName, key);
            log.debug("广播缓存失效: cacheName={}, key={}", cacheName, key);
        } catch (Exception e) {
            log.error("广播缓存失效失败: cacheName={}, key={}", cacheName, key, e);
        }
    }

    // ==================== 活动事件 ====================

    /**
     * 广播活动状态变更
     */
    public void broadcastActivityChange(int activityId, int status, Object data) {
        try {
            String jsonData = data != null ? JsonUtil.toJson(data) : null;
            distributedEventService.publishActivityChange(activityId, status, jsonData);
            log.info("广播活动变更: activityId={}, status={}", activityId, status);
        } catch (Exception e) {
            log.error("广播活动变更失败: activityId={}", activityId, e);
        }
    }

    // ==================== 玩家事件 ====================

    /**
     * 广播玩家上线
     */
    public void broadcastPlayerOnline(long roleId, int serverId) {
        try {
            distributedEventService.publishPlayerOnline(roleId, serverId);
            log.debug("广播玩家上线: roleId={}, serverId={}", roleId, serverId);
        } catch (Exception e) {
            log.error("广播玩家上线失败: roleId={}", roleId, e);
        }
    }

    /**
     * 广播玩家下线
     */
    public void broadcastPlayerOffline(long roleId, int serverId) {
        try {
            distributedEventService.publishPlayerOffline(roleId, serverId);
            log.debug("广播玩家下线: roleId={}, serverId={}", roleId, serverId);
        } catch (Exception e) {
            log.error("广播玩家下线失败: roleId={}", roleId, e);
        }
    }

    /**
     * 广播玩家数据变更
     */
    public void broadcastPlayerChange(long roleId, String changeType, Object data) {
        try {
            String jsonData = data != null ? JsonUtil.toJson(data) : null;
            distributedEventService.publishPlayerChange(roleId, changeType, jsonData);
            log.debug("广播玩家变更: roleId={}, type={}", roleId, changeType);
        } catch (Exception e) {
            log.error("广播玩家变更失败: roleId={}, type={}", roleId, changeType, e);
        }
    }

    // ==================== 公会事件 ====================

    /**
     * 广播公会成员变更
     */
    public void broadcastGuildMemberChange(long guildId, long roleId, int changeType) {
        try {
            distributedEventService.publishGuildMemberChange(guildId, roleId, changeType);
            log.debug("广播公会成员变更: guildId={}, roleId={}, type={}", guildId, roleId, changeType);
        } catch (Exception e) {
            log.error("广播公会成员变更失败: guildId={}", guildId, e);
        }
    }

    /**
     * 广播公会解散
     */
    public void broadcastGuildDissolve(long guildId) {
        try {
            distributedEventService.publishGuildDissolve(guildId);
            log.info("广播公会解散: guildId={}", guildId);
        } catch (Exception e) {
            log.error("广播公会解散失败: guildId={}", guildId, e);
        }
    }

    // ==================== 系统事件 ====================

    /**
     * 广播服务器维护通知
     */
    public void broadcastMaintenanceNotice(long maintenanceTime, int durationMinutes, String message) {
        try {
            distributedEventService.publishMaintenanceNotice(maintenanceTime, durationMinutes, message);
            log.info("广播维护通知: time={}, duration={}min", maintenanceTime, durationMinutes);
        } catch (Exception e) {
            log.error("广播维护通知失败", e);
        }
    }

    /**
     * 广播通用事件
     */
    public void broadcast(String eventType, Object eventData) {
        try {
            String jsonData = eventData != null ? JsonUtil.toJson(eventData) : null;
            distributedEventService.publishEvent(eventType, jsonData);
            log.debug("广播事件: type={}", eventType);
        } catch (Exception e) {
            log.error("广播事件失败: type={}", eventType, e);
        }
    }

    /**
     * 广播事件对象
     */
    public void broadcast(GameEvent event) {
        broadcast(event.getEventType(), event);
    }
}
