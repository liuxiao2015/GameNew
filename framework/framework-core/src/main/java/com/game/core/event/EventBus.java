package com.game.core.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * 游戏事件总线
 * <p>
 * 提供轻量级的事件发布/订阅机制，用于游戏内部事件通信：
 * <ul>
 *     <li>支持同步/异步事件处理</li>
 *     <li>支持事件处理优先级</li>
 *     <li>基于注解自动注册监听器</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 发布事件
 * eventBus.publish(new PlayerLevelUpEvent(roleId, oldLevel, newLevel));
 *
 * // 监听事件 (自动注册)
 * @Component
 * public class RewardService {
 *     @EventListener
 *     public void onLevelUp(PlayerLevelUpEvent event) {
 *         // 发放升级奖励
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventBus {

    private final ApplicationContext applicationContext;

    /**
     * 事件类型 -> 监听器列表
     */
    private final Map<Class<?>, List<ListenerInfo>> listeners = new ConcurrentHashMap<>();

    /**
     * 异步事件执行器
     */
    private ExecutorService asyncExecutor;

    @PostConstruct
    public void init() {
        asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        scanEventListeners();
        log.info("EventBus 初始化完成，共注册 {} 个事件类型", listeners.size());
    }

    @PreDestroy
    public void destroy() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }

    /**
     * 发布事件 (同步)
     *
     * @param event 事件
     */
    public void publish(GameEvent event) {
        if (event == null) {
            return;
        }

        List<ListenerInfo> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null || eventListeners.isEmpty()) {
            log.debug("事件无监听器: {}", event.getEventType());
            return;
        }

        long startTime = System.currentTimeMillis();

        for (ListenerInfo info : eventListeners) {
            if (event.isCancelled()) {
                break;
            }

            try {
                if (info.async) {
                    asyncExecutor.execute(() -> invokeListener(info, event));
                } else {
                    invokeListener(info, event);
                }
            } catch (Exception e) {
                log.error("事件处理异常: event={}, listener={}.{}",
                        event.getEventType(), info.bean.getClass().getSimpleName(),
                        info.method.getName(), e);
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        if (cost > 50) {
            log.warn("事件处理耗时过长: event={}, cost={}ms", event.getEventType(), cost);
        }
    }

    /**
     * 异步发布事件
     *
     * @param event 事件
     */
    public void publishAsync(GameEvent event) {
        asyncExecutor.execute(() -> publish(event));
    }

    /**
     * 发布事件并等待完成
     *
     * @param event 事件
     * @return CompletableFuture
     */
    public CompletableFuture<Void> publishAndWait(GameEvent event) {
        return CompletableFuture.runAsync(() -> publish(event), asyncExecutor);
    }

    /**
     * 手动注册监听器
     */
    public <T extends GameEvent> void register(Class<T> eventType, java.util.function.Consumer<T> handler) {
        register(eventType, handler, 0, false);
    }

    /**
     * 手动注册监听器 (带优先级和异步标志)
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void register(Class<T> eventType, java.util.function.Consumer<T> handler,
                                                int priority, boolean async) {
        List<ListenerInfo> list = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());

        // 创建代理对象和方法
        Object proxyBean = new Object() {
            public void handle(T event) {
                handler.accept(event);
            }
        };

        try {
            Method method = proxyBean.getClass().getMethod("handle", eventType);
            ListenerInfo info = new ListenerInfo(proxyBean, method, priority, async);
            list.add(info);
            list.sort(Comparator.comparingInt(a -> -a.priority));
        } catch (NoSuchMethodException e) {
            log.error("注册监听器失败", e);
        }
    }

    /**
     * 扫描并注册事件监听器
     */
    private void scanEventListeners() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

        for (Object bean : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {
                EventListener annotation = method.getAnnotation(EventListener.class);
                if (annotation == null) {
                    continue;
                }

                // 检查参数
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1 || !GameEvent.class.isAssignableFrom(paramTypes[0])) {
                    log.warn("EventListener 方法参数错误，必须有且只有一个 GameEvent 子类参数: {}.{}",
                            targetClass.getSimpleName(), method.getName());
                    continue;
                }

                Class<?> eventType = paramTypes[0];
                int priority = annotation.priority();
                boolean async = annotation.async();

                List<ListenerInfo> list = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
                list.add(new ListenerInfo(bean, method, priority, async));

                log.debug("注册事件监听器: event={}, listener={}.{}, priority={}, async={}",
                        eventType.getSimpleName(), targetClass.getSimpleName(),
                        method.getName(), priority, async);
            }
        }

        // 按优先级排序
        listeners.values().forEach(list ->
                list.sort(Comparator.comparingInt(a -> -a.priority)));
    }

    /**
     * 调用监听器
     */
    private void invokeListener(ListenerInfo info, GameEvent event) {
        try {
            info.method.invoke(info.bean, event);
        } catch (Exception e) {
            log.error("调用事件监听器异常: event={}, listener={}.{}",
                    event.getEventType(), info.bean.getClass().getSimpleName(),
                    info.method.getName(), e);
        }
    }

    /**
     * 监听器信息
     */
    private record ListenerInfo(Object bean, Method method, int priority, boolean async) {
    }
}
