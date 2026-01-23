package com.game.core.broadcast;

import com.game.api.broadcast.BroadcastService;
import com.game.core.cache.CacheService;
import com.game.core.config.game.ConfigLoader;
import com.game.core.event.EventBus;
import com.game.core.event.events.ConfigReloadEvent;
import com.game.core.push.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 广播服务适配器
 * <p>
 * 接收 Dubbo 广播调用，转换为本地操作：
 * <ul>
 *     <li>配置刷新 -> ConfigLoader.reload()</li>
 *     <li>缓存失效 -> CacheService.evict()</li>
 *     <li>服务器公告 -> PushService.broadcast()</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class BroadcastServiceAdapter implements BroadcastService {

    private final ConfigLoader configLoader;
    private final CacheService cacheService;
    private final PushService pushService;
    private final EventBus eventBus;

    @Override
    public void onConfigReload(String configName, long version) {
        log.info("收到配置刷新广播: configName={}, version={}", configName, version);
        try {
            configLoader.reload(configName, "broadcast");
            eventBus.publish(new com.game.core.event.events.ConfigReloadEvent(configName, "broadcast"));
        } catch (Exception e) {
            log.error("配置刷新失败: configName={}", configName, e);
        }
    }

    @Override
    public void onCacheEvict(String cacheName, String key) {
        log.info("收到缓存失效广播: cacheName={}, key={}", cacheName, key);
        try {
            if (key == null || key.isEmpty()) {
                cacheService.evictAll(cacheName);
            } else {
                cacheService.evict(cacheName, key);
            }
        } catch (Exception e) {
            log.error("缓存失效处理失败: cacheName={}, key={}", cacheName, key, e);
        }
    }

    @Override
    public void onServerNotice(int noticeType, String content, int duration) {
        log.info("收到服务器公告广播: type={}, content={}, duration={}",
                noticeType, content, duration);
        // 发布本地事件，由具体模块处理
        eventBus.publish(new ServerNoticeEvent(noticeType, content, duration));
    }

    @Override
    public void onActivityChange(int activityId, int status) {
        log.info("收到活动变更广播: activityId={}, status={}", activityId, status);
        eventBus.publish(new ActivityChangeEvent(activityId, status));
    }

    @Override
    public void onGlobalMail(long mailId) {
        log.info("收到全服邮件广播: mailId={}", mailId);
        eventBus.publish(new GlobalMailEvent(mailId));
    }

    @Override
    public void onMaintenanceNotice(long maintenanceTime, int durationMinutes) {
        log.info("收到维护通知广播: maintenanceTime={}, duration={}min",
                maintenanceTime, durationMinutes);
        eventBus.publish(new MaintenanceNoticeEvent(maintenanceTime, durationMinutes));
    }

    @Override
    public void onKickPlayer(long roleId, String reason) {
        log.info("收到踢人广播: roleId={}, reason={}", roleId, reason);
        eventBus.publish(new KickPlayerEvent(roleId, reason));
    }

    @Override
    public void onBanPlayer(long roleId, int banType, long banUntil) {
        log.info("收到封禁广播: roleId={}, banType={}, banUntil={}",
                roleId, banType, banUntil);
        eventBus.publish(new BanPlayerEvent(roleId, banType, banUntil));
    }

    // ==================== 事件定义 ====================

    public record ServerNoticeEvent(int noticeType, String content, int duration)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "ServerNotice";
        }
    }

    public record ActivityChangeEvent(int activityId, int status)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "ActivityChange";
        }
    }

    public record GlobalMailEvent(long mailId)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "GlobalMail";
        }
    }

    public record MaintenanceNoticeEvent(long maintenanceTime, int durationMinutes)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "MaintenanceNotice";
        }
    }

    public record KickPlayerEvent(long roleId, String reason)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "KickPlayer";
        }
    }

    public record BanPlayerEvent(long roleId, int banType, long banUntil)
            implements com.game.core.event.GameEvent {
        @Override
        public String getEventType() {
            return "BanPlayer";
        }
    }
}
