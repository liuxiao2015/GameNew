package com.game.actor.core;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版 Actor 基类
 * <p>
 * 支持注解驱动的消息处理，简化开发
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * public class PlayerActor extends BaseActor<PlayerData> {
 *
 *     public PlayerActor(long roleId) {
 *         super(roleId, "Player", 1000);
 *     }
 *
 *     @MessageHandler("LOGIN")
 *     public void onLogin(LoginData data) {
 *         // 处理登录
 *         getData().setLastLoginTime(System.currentTimeMillis());
 *         markDirty();
 *     }
 *
 *     @MessageHandler("ADD_GOLD")
 *     public AddGoldResult onAddGold(AddGoldData data) {
 *         long current = getData().getGold();
 *         long newGold = current + data.getAmount();
 *         getData().setGold(newGold);
 *         markDirty();
 *         return new AddGoldResult(true, newGold);
 *     }
 *
 *     @MessageHandler(value = "DAILY_RESET", async = true)
 *     public void onDailyReset() {
 *         // 每日重置逻辑
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> Actor 数据类型
 * @author GameServer
 */
@Slf4j
public abstract class BaseActor<T> extends Actor<T> {

    /**
     * 消息处理器映射
     */
    private final Map<String, MessageHandlerInfo> handlers = new ConcurrentHashMap<>();

    protected BaseActor(long actorId, String actorType, int maxMailboxSize) {
        super(actorId, actorType, maxMailboxSize);
        // 扫描注册消息处理器
        registerMessageHandlers();
    }

    /**
     * 扫描并注册消息处理器
     */
    private void registerMessageHandlers() {
        Class<?> clazz = this.getClass();
        for (Method method : clazz.getMethods()) {
            MessageHandler annotation = method.getAnnotation(MessageHandler.class);
            if (annotation != null) {
                String messageType = annotation.value();
                boolean async = annotation.async();
                
                handlers.put(messageType, new MessageHandlerInfo(method, async));
                
                if (log.isDebugEnabled()) {
                    log.debug("注册消息处理器: actorType={}, messageType={}, method={}",
                            getActorType(), messageType, method.getName());
                }
            }
        }
    }

    @Override
    protected void handleMessage(ActorMessage message) {
        String messageType = message.getType();
        MessageHandlerInfo handlerInfo = handlers.get(messageType);

        if (handlerInfo != null) {
            invokeHandler(handlerInfo, message);
        } else {
            // 调用子类的默认处理方法
            onMessage(message);
        }
    }

    /**
     * 调用消息处理器
     */
    private void invokeHandler(MessageHandlerInfo handlerInfo, ActorMessage message) {
        try {
            Method method = handlerInfo.method();
            Object[] args = buildArgs(method, message);
            
            Object result = method.invoke(this, args);
            
            // 如果有 Future，设置结果
            if (message.getFuture() != null && result != null) {
                message.getFuture().complete(result);
            }
            
        } catch (Exception e) {
            log.error("处理消息异常: actorId={}, messageType={}", getActorId(), message.getType(), e);
            
            // 如果有 Future，设置异常
            if (message.getFuture() != null) {
                message.getFuture().completeExceptionally(e);
            }
        }
    }

    /**
     * 构建方法参数
     */
    private Object[] buildArgs(Method method, ActorMessage message) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return new Object[0];
        }

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            
            if (ActorMessage.class.isAssignableFrom(paramType)) {
                args[i] = message;
            } else if (paramType.isInstance(message.getData())) {
                args[i] = message.getData();
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * 默认消息处理 (子类可覆盖)
     */
    protected void onMessage(ActorMessage message) {
        log.warn("未处理的消息类型: actorId={}, messageType={}", getActorId(), message.getType());
    }

    // ==================== 便捷方法 ====================

    /**
     * 发送消息并等待结果
     *
     * @param messageType 消息类型
     * @param data        消息数据
     * @param <R>         结果类型
     * @return CompletableFuture
     */
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> ask(String messageType, Object data) {
        CompletableFuture<R> future = new CompletableFuture<>();
        ActorMessage message = ActorMessage.of(messageType, data, (CompletableFuture<Object>) future);
        tell(message);
        return future;
    }

    /**
     * 发送消息 (fire-and-forget)
     *
     * @param messageType 消息类型
     * @param data        消息数据
     */
    public void fire(String messageType, Object data) {
        tell(ActorMessage.of(messageType, data));
    }

    /**
     * 发送消息 (fire-and-forget)
     *
     * @param messageType 消息类型
     */
    public void fire(String messageType) {
        tell(ActorMessage.of(messageType, null));
    }

    /**
     * 消息处理器信息
     */
    private record MessageHandlerInfo(Method method, boolean async) {
    }
}
