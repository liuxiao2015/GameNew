package com.game.actor.remote;

import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import com.game.actor.core.ActorSystemRegistry;
import com.game.api.actor.ActorRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 远程 Actor 消息投递服务实现
 * <p>
 * 接收来自其他 JVM 的 Actor 消息，并路由到本地 ActorSystem。
 * 增强: 支持批量投递和 ActorSystem 名称查询 (供集群分片使用)。
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", timeout = 5000)
public class ActorRemoteServiceImpl implements ActorRemoteService {

    @Override
    public boolean tell(String actorSystemName, long actorId, String messageType, String jsonData) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            log.warn("远程消息路由失败, ActorSystem 不存在: name={}, actorId={}", actorSystemName, actorId);
            return false;
        }

        ActorMessage message = ActorMessage.of(messageType, jsonData);
        message.setSourceActorType("REMOTE");
        return actorSystem.tell(actorId, message);
    }

    @Override
    public String ask(String actorSystemName, long actorId, String messageType, String jsonData, long timeoutMs) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            log.warn("远程 ask 路由失败, ActorSystem 不存在: name={}, actorId={}", actorSystemName, actorId);
            return null;
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        ActorMessage message = ActorMessage.of(messageType, jsonData, future);
        message.setSourceActorType("REMOTE");

        boolean sent = actorSystem.tell(actorId, message);
        if (!sent) {
            return null;
        }

        try {
            Object result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("远程 ask 超时或异常: actorSystem={}, actorId={}, msgType={}",
                    actorSystemName, actorId, messageType, e);
            return null;
        }
    }

    @Override
    public boolean hasActor(String actorSystemName, long actorId) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            return false;
        }
        return actorSystem.hasActor(actorId);
    }

    @Override
    public int batchTell(String actorSystemName, String actorIds, String messageType, String jsonData) {
        ActorSystem<?> actorSystem = ActorSystemRegistry.get(actorSystemName);
        if (actorSystem == null) {
            log.warn("远程批量消息路由失败, ActorSystem 不存在: name={}", actorSystemName);
            return 0;
        }

        if (actorIds == null || actorIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String idStr : actorIds.split(",")) {
            try {
                long actorId = Long.parseLong(idStr.trim());
                ActorMessage message = ActorMessage.of(messageType, jsonData);
                message.setSourceActorType("REMOTE");
                if (actorSystem.tell(actorId, message)) {
                    successCount++;
                }
            } catch (NumberFormatException e) {
                log.warn("批量投递跳过无效 actorId: {}", idStr);
            }
        }

        log.debug("远程批量投递完成: actorSystem={}, total={}, success={}",
                actorSystemName, actorIds.split(",").length, successCount);
        return successCount;
    }

    @Override
    public String getActorSystemNames() {
        return String.join(",", ActorSystemRegistry.getNames());
    }
}
