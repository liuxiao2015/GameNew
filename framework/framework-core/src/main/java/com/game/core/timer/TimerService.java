package com.game.core.timer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 定时器服务
 * <p>
 * 提供游戏内的定时任务管理，支持多机部署：
 * <ul>
 *     <li>延迟执行任务</li>
 *     <li>周期性任务</li>
 *     <li>玩家级别的定时器 (与 Actor 生命周期绑定)</li>
 * </ul>
 * </p>
 *
 * <b>注意：</b>
 * <ul>
 *     <li>本服务的定时器存储在本地内存，服务重启会丢失</li>
 *     <li>适用于 Buff 倒计时、技能冷却等短期定时任务</li>
 *     <li>长期/全局任务请使用 XXL-Job 等分布式调度</li>
 *     <li>玩家定时器应与 Actor 生命周期绑定，玩家下线时自动清理</li>
 * </ul>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 延迟 5 秒执行
 * timerService.schedule(GameTimer.builder("buff-expire-" + buffId)
 *     .roleId(roleId)
 *     .timerType("BUFF_EXPIRE")
 *     .delay(5, TimeUnit.SECONDS)
 *     .once()
 *     .callback(t -> expireBuff(buffId))
 *     .build());
 *
 * // 每 10 秒执行一次，最多 6 次
 * timerService.schedule(GameTimer.builder("energy-recover-" + roleId)
 *     .roleId(roleId)
 *     .timerType("ENERGY_RECOVER")
 *     .delay(10, TimeUnit.SECONDS)
 *     .period(10, TimeUnit.SECONDS)
 *     .maxExecuteCount(6)
 *     .callback(t -> recoverEnergy(roleId))
 *     .build());
 *
 * // 取消定时器
 * timerService.cancel("buff-expire-" + buffId);
 *
 * // 取消玩家所有定时器 (玩家下线时调用)
 * timerService.cancelByRoleId(roleId);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class TimerService {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 定时器存储 (timerId -> GameTimer)
     */
    private final Map<String, GameTimer> timers = new ConcurrentHashMap<>();

    /**
     * 玩家定时器索引 (roleId -> Set<timerId>)
     */
    private final Map<Long, Set<String>> roleTimerIndex = new ConcurrentHashMap<>();

    /**
     * 调度器
     */
    private ScheduledExecutorService scheduler;

    /**
     * 定时器检查间隔 (毫秒)
     */
    private static final long CHECK_INTERVAL_MS = 100;

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "timer-service");
            t.setDaemon(true);
            return t;
        });

        // 启动定时检查
        scheduler.scheduleAtFixedRate(this::tick, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("TimerService 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        timers.clear();
        roleTimerIndex.clear();
        log.info("TimerService 已关闭");
    }

    /**
     * 调度定时器
     */
    public void schedule(GameTimer timer) {
        if (timer == null || timer.isCancelled()) {
            return;
        }

        timers.put(timer.getTimerId(), timer);

        // 建立玩家索引
        if (timer.getRoleId() > 0) {
            roleTimerIndex.computeIfAbsent(timer.getRoleId(), k -> ConcurrentHashMap.newKeySet())
                    .add(timer.getTimerId());
        }

        log.debug("调度定时器: timerId={}, roleId={}, type={}, delay={}ms, period={}ms",
                timer.getTimerId(), timer.getRoleId(), timer.getTimerType(),
                timer.getDelay(), timer.getPeriod());
    }

    /**
     * 延迟执行一次性任务 (便捷方法)
     */
    public String scheduleOnce(String timerId, long delay, TimeUnit unit, Runnable task) {
        schedule(GameTimer.builder(timerId)
                .delay(delay, unit)
                .once()
                .callback(task)
                .build());
        return timerId;
    }

    /**
     * 延迟执行一次性任务 (带角色)
     */
    public String scheduleOnce(String timerId, long roleId, long delay, TimeUnit unit, Runnable task) {
        schedule(GameTimer.builder(timerId)
                .roleId(roleId)
                .delay(delay, unit)
                .once()
                .callback(task)
                .build());
        return timerId;
    }

    /**
     * 调度周期性任务 (便捷方法)
     */
    public String schedulePeriodic(String timerId, long delay, long period, TimeUnit unit, Runnable task) {
        schedule(GameTimer.builder(timerId)
                .delay(delay, unit)
                .period(period, unit)
                .callback(task)
                .build());
        return timerId;
    }

    /**
     * 取消定时器
     */
    public boolean cancel(String timerId) {
        GameTimer timer = timers.remove(timerId);
        if (timer != null) {
            timer.cancel();

            // 清理玩家索引
            if (timer.getRoleId() > 0) {
                Set<String> timerIds = roleTimerIndex.get(timer.getRoleId());
                if (timerIds != null) {
                    timerIds.remove(timerId);
                }
            }

            log.debug("取消定时器: timerId={}", timerId);
            return true;
        }
        return false;
    }

    /**
     * 取消玩家所有定时器
     */
    public void cancelByRoleId(long roleId) {
        Set<String> timerIds = roleTimerIndex.remove(roleId);
        if (timerIds != null && !timerIds.isEmpty()) {
            for (String timerId : timerIds) {
                GameTimer timer = timers.remove(timerId);
                if (timer != null) {
                    timer.cancel();
                }
            }
            log.debug("取消玩家所有定时器: roleId={}, count={}", roleId, timerIds.size());
        }
    }

    /**
     * 取消指定类型的所有定时器
     */
    public void cancelByType(String timerType) {
        List<String> toRemove = new ArrayList<>();
        for (GameTimer timer : timers.values()) {
            if (timerType.equals(timer.getTimerType())) {
                toRemove.add(timer.getTimerId());
            }
        }
        toRemove.forEach(this::cancel);
    }

    /**
     * 获取定时器
     */
    public GameTimer getTimer(String timerId) {
        return timers.get(timerId);
    }

    /**
     * 检查定时器是否存在
     */
    public boolean exists(String timerId) {
        return timers.containsKey(timerId);
    }

    /**
     * 获取定时器剩余时间 (毫秒)
     */
    public long getRemainingTime(String timerId) {
        GameTimer timer = timers.get(timerId);
        return timer != null ? timer.getRemainingTime() : -1;
    }

    /**
     * 获取当前定时器数量
     */
    public int getTimerCount() {
        return timers.size();
    }

    /**
     * 定时检查并执行
     */
    private void tick() {
        long currentTime = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (GameTimer timer : timers.values()) {
            if (timer.isCancelled()) {
                toRemove.add(timer.getTimerId());
                continue;
            }

            if (timer.shouldExecute(currentTime)) {
                try {
                    timer.execute();

                    if (timer.isCancelled()) {
                        toRemove.add(timer.getTimerId());
                    }
                } catch (Exception e) {
                    log.error("定时器执行异常: timerId={}, type={}",
                            timer.getTimerId(), timer.getTimerType(), e);
                }
            }
        }

        // 清理已取消的定时器
        for (String timerId : toRemove) {
            cancel(timerId);
        }
    }
}
