package com.game.service.chat.listener;

import com.game.core.event.EventListener;
import com.game.data.redis.RedisService;
import com.game.service.chat.service.ChatBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 聊天事件监听器
 * <p>
 * 监听游戏事件，触发相关的系统消息推送
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - EventListener: 声明式事件监听
 * - ChatBusinessService: 发送系统消息
 * - RedisService: 订阅跨服聊天消息
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatBusinessService chatBusinessService;
    private final RedisService redisService;

    /**
     * 玩家升级通知 (监听玩家升级事件，发送世界公告)
     * <p>
     * 注意：这里需要在 service-chat 中定义对应的事件类或使用通用事件
     * </p>
     */
    // @EventListener
    // public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    //     if (event.getNewLevel() % 10 == 0) { // 每 10 级发一次公告
    //         String content = String.format("恭喜玩家 %s 达到 %d 级！", 
    //                 event.getRoleName(), event.getNewLevel());
    //         chatBusinessService.sendSystemMessage(content, 1);
    //     }
    // }

    /**
     * 公会创建通知
     */
    // @EventListener
    // public void onGuildCreated(GuildCreatedEvent event) {
    //     String content = String.format("公会 【%s】 创建成功！会长：%s",
    //             event.getGuildName(), event.getLeaderName());
    //     chatBusinessService.sendSystemMessage(content, 1);
    // }

    /**
     * 首杀公告
     */
    // @EventListener
    // public void onBossFirstKill(BossFirstKillEvent event) {
    //     String content = String.format("恭喜玩家 %s 首次击杀 %s！",
    //             event.getPlayerName(), event.getBossName());
    //     chatBusinessService.sendSystemMessage(content, 2); // 2 = 弹窗
    // }

    /**
     * 活动开始通知
     */
    public void onActivityStart(String activityName) {
        String content = String.format("活动 【%s】 已经开始！快来参与吧！", activityName);
        chatBusinessService.sendSystemMessage(content, 1);
    }

    /**
     * 维护公告
     */
    public void sendMaintenanceNotice(long duration, String reason) {
        String content = String.format("服务器将在 %d 分钟后进行维护，预计维护时间 %s。原因：%s",
                duration / 60000, formatDuration(duration), reason);
        chatBusinessService.sendSystemMessage(content, 2);
    }

    private String formatDuration(long millis) {
        long minutes = millis / 60000;
        if (minutes < 60) {
            return minutes + "分钟";
        }
        return (minutes / 60) + "小时" + (minutes % 60) + "分钟";
    }
}
