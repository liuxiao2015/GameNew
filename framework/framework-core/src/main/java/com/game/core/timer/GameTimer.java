package com.game.core.timer;

import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 游戏定时器
 * <p>
 * 用于延迟执行或周期性执行任务
 * </p>
 *
 * @author GameServer
 */
@Getter
public class GameTimer {

    /**
     * 定时器 ID
     */
    private final String timerId;

    /**
     * 关联的角色 ID (0 表示全局定时器)
     */
    private final long roleId;

    /**
     * 定时器类型
     */
    private final String timerType;

    /**
     * 首次执行延迟 (毫秒)
     */
    private final long delay;

    /**
     * 执行周期 (毫秒，0 表示只执行一次)
     */
    private final long period;

    /**
     * 创建时间
     */
    private final long createTime;

    /**
     * 下次执行时间
     */
    private volatile long nextExecuteTime;

    /**
     * 已执行次数
     */
    private volatile int executeCount;

    /**
     * 最大执行次数 (0 表示无限制)
     */
    private final int maxExecuteCount;

    /**
     * 是否已取消
     */
    private volatile boolean cancelled;

    /**
     * 执行回调
     */
    private final Consumer<GameTimer> callback;

    /**
     * 扩展数据
     */
    private final Object data;

    private GameTimer(Builder builder) {
        this.timerId = builder.timerId;
        this.roleId = builder.roleId;
        this.timerType = builder.timerType;
        this.delay = builder.delay;
        this.period = builder.period;
        this.maxExecuteCount = builder.maxExecuteCount;
        this.callback = builder.callback;
        this.data = builder.data;
        this.createTime = System.currentTimeMillis();
        this.nextExecuteTime = this.createTime + this.delay;
        this.executeCount = 0;
        this.cancelled = false;
    }

    /**
     * 检查是否应该执行
     */
    public boolean shouldExecute(long currentTime) {
        return !cancelled && currentTime >= nextExecuteTime;
    }

    /**
     * 执行定时器
     */
    public void execute() {
        if (cancelled) {
            return;
        }

        executeCount++;
        callback.accept(this);

        // 检查是否达到最大执行次数
        if (maxExecuteCount > 0 && executeCount >= maxExecuteCount) {
            cancelled = true;
            return;
        }

        // 更新下次执行时间
        if (period > 0) {
            nextExecuteTime = System.currentTimeMillis() + period;
        } else {
            cancelled = true; // 一次性定时器
        }
    }

    /**
     * 取消定时器
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 获取剩余时间 (毫秒)
     */
    public long getRemainingTime() {
        return Math.max(0, nextExecuteTime - System.currentTimeMillis());
    }

    /**
     * 判断是否是周期性定时器
     */
    public boolean isPeriodic() {
        return period > 0;
    }

    /**
     * 获取扩展数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    // ==================== Builder ====================

    public static Builder builder(String timerId) {
        return new Builder(timerId);
    }

    public static class Builder {
        private final String timerId;
        private long roleId = 0;
        private String timerType = "DEFAULT";
        private long delay = 0;
        private long period = 0;
        private int maxExecuteCount = 0;
        private Consumer<GameTimer> callback;
        private Object data;

        public Builder(String timerId) {
            this.timerId = timerId;
        }

        public Builder roleId(long roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder timerType(String timerType) {
            this.timerType = timerType;
            return this;
        }

        public Builder delay(long delay, TimeUnit unit) {
            this.delay = unit.toMillis(delay);
            return this;
        }

        public Builder delayMs(long delayMs) {
            this.delay = delayMs;
            return this;
        }

        public Builder period(long period, TimeUnit unit) {
            this.period = unit.toMillis(period);
            return this;
        }

        public Builder periodMs(long periodMs) {
            this.period = periodMs;
            return this;
        }

        public Builder maxExecuteCount(int count) {
            this.maxExecuteCount = count;
            return this;
        }

        public Builder once() {
            this.period = 0;
            this.maxExecuteCount = 1;
            return this;
        }

        public Builder callback(Consumer<GameTimer> callback) {
            this.callback = callback;
            return this;
        }

        public Builder callback(Runnable runnable) {
            this.callback = t -> runnable.run();
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public GameTimer build() {
            if (callback == null) {
                throw new IllegalArgumentException("callback is required");
            }
            return new GameTimer(this);
        }
    }
}
