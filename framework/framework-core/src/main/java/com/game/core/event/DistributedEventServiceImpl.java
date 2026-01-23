package com.game.core.event;

import com.game.api.event.DistributedEventService;
import com.game.common.util.JsonUtil;
import com.game.core.cache.CacheService;
import com.game.core.config.game.ConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 分布式事件服务实现
 * <p>
 * 接收 Dubbo broadcast 调用，转换为本地事件处理
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class DistributedEventServiceImpl implements DistributedEventService {

    private final EventBus eventBus;
    private final ConfigLoader configLoader;
    private final CacheService cacheService;

    // ==================== 配置事件 ====================

    @Override
    public void publishConfigReload(String configName, long version) {
        log.info("收到配置刷新事件: configName={}, version={}", configName, version);
        try {
            configLoader.reload(configName, "distributed");
            eventBus.publish(new ConfigReloadEvent(configName, version));
        } catch (Exception e) {
            log.error("配置刷新失败: configName={}", configName, e);
        }
    }

    @Override
    public void publishCacheEvict(String cacheName, String key) {
        log.debug("收到缓存失效事件: cacheName={}, key={}", cacheName, key);
        try {
            if (key == null || key.isEmpty()) {
                cacheService.evictAll(cacheName);
            } else {
                cacheService.evict(cacheName, key);
            }
            eventBus.publish(new CacheEvictEvent(cacheName, key));
        } catch (Exception e) {
            log.error("缓存失效处理失败: cacheName={}, key={}", cacheName, key, e);
        }
    }

    // ==================== 活动事件 ====================

    @Override
    public void publishActivityChange(int activityId, int status, String data) {
        log.info("收到活动变更事件: activityId={}, status={}", activityId, status);
        eventBus.publish(new ActivityChangeEvent(activityId, status, data));
    }

    // ==================== 玩家事件 ====================

    @Override
    public void publishPlayerOnline(long roleId, int serverId) {
        log.debug("收到玩家上线事件: roleId={}, serverId={}", roleId, serverId);
        eventBus.publish(new PlayerOnlineEvent(roleId, serverId, true));
    }

    @Override
    public void publishPlayerOffline(long roleId, int serverId) {
        log.debug("收到玩家下线事件: roleId={}, serverId={}", roleId, serverId);
        eventBus.publish(new PlayerOnlineEvent(roleId, serverId, false));
    }

    @Override
    public void publishPlayerChange(long roleId, String changeType, String data) {
        log.debug("收到玩家变更事件: roleId={}, changeType={}", roleId, changeType);
        eventBus.publish(new PlayerChangeEvent(roleId, changeType, data));
    }

    // ==================== 公会事件 ====================

    @Override
    public void publishGuildMemberChange(long guildId, long roleId, int changeType) {
        log.debug("收到公会成员变更事件: guildId={}, roleId={}, changeType={}", 
                guildId, roleId, changeType);
        eventBus.publish(new GuildMemberChangeEvent(guildId, roleId, changeType));
    }

    @Override
    public void publishGuildDissolve(long guildId) {
        log.info("收到公会解散事件: guildId={}", guildId);
        eventBus.publish(new GuildDissolveEvent(guildId));
    }

    // ==================== 系统事件 ====================

    @Override
    public void publishMaintenanceNotice(long maintenanceTime, int durationMinutes, String message) {
        log.info("收到维护通知事件: maintenanceTime={}, duration={}min", 
                maintenanceTime, durationMinutes);
        eventBus.publish(new MaintenanceNoticeEvent(maintenanceTime, durationMinutes, message));
    }

    @Override
    public void publishEvent(String eventType, String eventData) {
        log.debug("收到通用事件: eventType={}", eventType);
        eventBus.publish(new GenericEvent(eventType, eventData));
    }

    // ==================== 事件定义 ====================

    public record ConfigReloadEvent(String configName, long version) implements GameEvent {
        @Override
        public String getEventType() {
            return "ConfigReload";
        }
    }

    public record CacheEvictEvent(String cacheName, String key) implements GameEvent {
        @Override
        public String getEventType() {
            return "CacheEvict";
        }
    }

    public record ActivityChangeEvent(int activityId, int status, String data) implements GameEvent {
        @Override
        public String getEventType() {
            return "ActivityChange";
        }
    }

    public record PlayerOnlineEvent(long roleId, int serverId, boolean online) implements GameEvent {
        @Override
        public String getEventType() {
            return online ? "PlayerOnline" : "PlayerOffline";
        }
    }

    public record PlayerChangeEvent(long roleId, String changeType, String data) implements GameEvent {
        @Override
        public String getEventType() {
            return "PlayerChange:" + changeType;
        }
    }

    public record GuildMemberChangeEvent(long guildId, long roleId, int changeType) implements GameEvent {
        @Override
        public String getEventType() {
            return "GuildMemberChange";
        }
    }

    public record GuildDissolveEvent(long guildId) implements GameEvent {
        @Override
        public String getEventType() {
            return "GuildDissolve";
        }
    }

    public record MaintenanceNoticeEvent(long maintenanceTime, int durationMinutes, String message) 
            implements GameEvent {
        @Override
        public String getEventType() {
            return "MaintenanceNotice";
        }
    }

    public record GenericEvent(String eventType, String data) implements GameEvent {
        @Override
        public String getEventType() {
            return eventType;
        }
    }
}
