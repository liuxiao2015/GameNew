package com.game.actor.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Actor 基类
 * <p>
 * 实现无锁化并发模型：
 * - 每个 Actor 实例对应一个实体 (玩家/公会等)
 * - 内部使用单线程顺序处理消息
 * - 通过消息队列实现线程安全
 * </p>
 * <p>
 * 增强能力：
 * - 监督策略 {@link SupervisorStrategy}: 异常时可选 RESUME/RESTART/STOP/ESCALATE
 * - 背压机制 {@link MailboxOverflowStrategy}: 邮箱满时可选 DROP_NEW/DROP_OLDEST/BLOCK/DYNAMIC_GROW
 * </p>
 *
 * @param <T> Actor 数据类型
 * @author GameServer
 */
@Slf4j
public abstract class Actor<T> implements Runnable {

    /**
     * Actor 状态
     */
    public enum State {
        INIT, RUNNING, STOPPED
    }

    @Getter
    private final long actorId;

    @Getter
    private final String actorType;

    @Getter
    protected volatile T data;

    /**
     * 消息队列 (邮箱)
     * <p>
     * 默认使用有界队列; DYNAMIC_GROW 策略下会切换为无界队列。
     * </p>
     */
    private volatile BlockingQueue<ActorMessage> mailbox;

    /**
     * 邮箱最大容量 (软上限)
     */
    private final int maxMailboxSize;

    /**
     * 邮箱溢出策略 (背压)
     */
    @Getter
    private final MailboxOverflowStrategy overflowStrategy;

    private volatile State state = State.INIT;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    @Getter
    private final AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());

    @Getter
    private final AtomicLong lastSaveTime = new AtomicLong(System.currentTimeMillis());

    @Getter
    private final long createTime = System.currentTimeMillis();

    /**
     * 连续异常计数 (用于监督策略判断)
     */
    @Getter
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);

    /**
     * 邮箱已动态扩容标记
     */
    private volatile boolean mailboxGrown = false;

    private ActorSystem<?> actorSystem;

    // ==================== 构造 ====================

    protected Actor(long actorId, String actorType, int maxMailboxSize) {
        this(actorId, actorType, maxMailboxSize, MailboxOverflowStrategy.DROP_NEW);
    }

    protected Actor(long actorId, String actorType, int maxMailboxSize, MailboxOverflowStrategy overflowStrategy) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.maxMailboxSize = maxMailboxSize;
        this.overflowStrategy = overflowStrategy;

        if (overflowStrategy == MailboxOverflowStrategy.DYNAMIC_GROW) {
            // 动态扩容模式使用无界队列
            this.mailbox = new LinkedBlockingQueue<>();
        } else {
            this.mailbox = new LinkedBlockingQueue<>(maxMailboxSize);
        }
    }

    // ==================== 抽象方法 ====================

    /**
     * 初始化 Actor (加载数据)
     */
    protected abstract T loadData();

    /**
     * 保存 Actor 数据
     */
    protected abstract void saveData();

    /**
     * 处理消息
     */
    protected abstract void handleMessage(ActorMessage message);

    /**
     * Actor 停止前的清理工作 (子类可覆盖)
     */
    protected void onStop() {
    }

    /**
     * 返回此 Actor 的监督策略 (子类可覆写以自定义)
     * <p>
     * 默认返回 null，表示使用 ActorSystem 的全局策略。
     * </p>
     */
    protected SupervisorStrategy supervisorStrategy() {
        return null;
    }

    // ==================== 生命周期 ====================

    void setActorSystem(ActorSystem<?> actorSystem) {
        this.actorSystem = actorSystem;
    }

    ActorSystem<?> getActorSystem() {
        return actorSystem;
    }

    /**
     * 启动 Actor
     */
    public void start() {
        if (state != State.INIT) {
            log.warn("Actor 无法启动，当前状态: actorId={}, state={}", actorId, state);
            return;
        }
        try {
            this.data = loadData();
            if (this.data == null) {
                log.error("Actor 数据加载失败: actorId={}", actorId);
                return;
            }
            this.state = State.RUNNING;
            log.info("Actor 启动成功: actorId={}, type={}", actorId, actorType);
        } catch (Exception e) {
            log.error("Actor 启动异常: actorId={}", actorId, e);
        }
    }

    /**
     * 停止 Actor
     */
    public void stop() {
        if (state != State.RUNNING) {
            return;
        }
        this.state = State.STOPPED;
        try {
            processRemainingMessages();
            if (dirty.get()) {
                saveData();
                dirty.set(false);
            }
            onStop();
            log.info("Actor 停止成功: actorId={}, type={}", actorId, actorType);
        } catch (Exception e) {
            log.error("Actor 停止异常: actorId={}", actorId, e);
        }
    }

    /**
     * 重启 Actor (重新加载数据, 保留邮箱)
     */
    void restart() {
        log.info("Actor 重启: actorId={}, type={}", actorId, actorType);
        try {
            // 先保存当前脏数据
            if (dirty.get()) {
                saveData();
                dirty.set(false);
            }
            // 重新加载
            T newData = loadData();
            if (newData != null) {
                this.data = newData;
                this.consecutiveErrors.set(0);
                log.info("Actor 重启成功: actorId={}", actorId);
            } else {
                log.error("Actor 重启失败 (数据加载返回 null): actorId={}", actorId);
            }
        } catch (Exception e) {
            log.error("Actor 重启异常: actorId={}", actorId, e);
        }
    }

    // ==================== 消息发送 (含背压) ====================

    /**
     * 发送消息到 Actor
     *
     * @param message 消息
     * @return 是否成功投递
     */
    public boolean tell(ActorMessage message) {
        if (state != State.RUNNING) {
            log.warn("Actor 未运行，消息被丢弃: actorId={}, msgType={}", actorId, message.getType());
            return false;
        }

        boolean offered = offerWithStrategy(message);
        if (offered) {
            lastActiveTime.set(System.currentTimeMillis());
            tryProcess();
        }
        return offered;
    }

    /**
     * 根据背压策略投递消息
     */
    private boolean offerWithStrategy(ActorMessage message) {
        // 队列未满，直接投递
        if (mailbox.size() < maxMailboxSize) {
            return mailbox.offer(message);
        }

        // 队列已满，按策略处理
        return switch (overflowStrategy) {
            case DROP_NEW -> {
                log.warn("Actor 邮箱已满，丢弃新消息: actorId={}, msgType={}, size={}",
                        actorId, message.getType(), mailbox.size());
                yield false;
            }
            case DROP_OLDEST -> {
                ActorMessage dropped = mailbox.poll();
                if (dropped != null) {
                    log.warn("Actor 邮箱已满，丢弃最旧消息: actorId={}, droppedType={}, newType={}",
                            actorId, dropped.getType(), message.getType());
                }
                yield mailbox.offer(message);
            }
            case BLOCK_WITH_TIMEOUT -> {
                try {
                    boolean ok = mailbox.offer(message, 3, TimeUnit.SECONDS);
                    if (!ok) {
                        log.warn("Actor 邮箱已满且等待超时: actorId={}, msgType={}", actorId, message.getType());
                    }
                    yield ok;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    yield false;
                }
            }
            case DYNAMIC_GROW -> {
                // 无界队列，直接投递，但超过软上限打印告警
                if (!mailboxGrown && mailbox.size() >= maxMailboxSize) {
                    mailboxGrown = true;
                    log.warn("Actor 邮箱超过软上限，已动态扩容: actorId={}, size={}, softLimit={}",
                            actorId, mailbox.size(), maxMailboxSize);
                }
                yield mailbox.offer(message);
            }
        };
    }

    // ==================== 消息处理 (含监督策略) ====================

    private void tryProcess() {
        if (processing.compareAndSet(false, true)) {
            if (actorSystem != null) {
                actorSystem.execute(this);
            } else {
                run();
            }
        }
    }

    @Override
    public void run() {
        try {
            processMessages();
        } finally {
            processing.set(false);
            if (!mailbox.isEmpty() && state == State.RUNNING) {
                tryProcess();
            }
        }
    }

    private void processMessages() {
        int processedCount = 0;
        int maxBatchSize = 100;

        while (processedCount < maxBatchSize && state == State.RUNNING) {
            try {
                ActorMessage message = mailbox.poll(10, TimeUnit.MILLISECONDS);
                if (message == null) {
                    break;
                }
                handleMessageSafe(message);
                processedCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processRemainingMessages() {
        while (!mailbox.isEmpty()) {
            ActorMessage message = mailbox.poll();
            if (message != null) {
                handleMessageSafe(message);
            }
        }
    }

    /**
     * 安全地处理消息 (集成监督策略)
     */
    private void handleMessageSafe(ActorMessage message) {
        try {
            handleMessage(message);
            // 成功则重置连续异常计数
            consecutiveErrors.set(0);
        } catch (Exception e) {
            consecutiveErrors.incrementAndGet();
            log.error("Actor 处理消息异常: actorId={}, msgType={}, consecutiveErrors={}",
                    actorId, message.getType(), consecutiveErrors.get(), e);

            // 查找监督策略: Actor 级别 > ActorSystem 全局
            SupervisorStrategy strategy = supervisorStrategy();
            if (strategy == null && actorSystem != null) {
                strategy = actorSystem.getSupervisorStrategy();
            }
            if (strategy == null) {
                strategy = SupervisorStrategy.defaultStrategy();
            }

            SupervisorStrategy.Directive directive = strategy.decide(this, message, e);
            executeDirective(directive, message, e);
        }
    }

    /**
     * 执行监督指令
     */
    private void executeDirective(SupervisorStrategy.Directive directive, ActorMessage message, Exception error) {
        switch (directive) {
            case RESUME -> {
                // 默认: 不做额外处理，继续下一条消息
                log.debug("SupervisorStrategy: RESUME actorId={}", actorId);
            }
            case RESTART -> {
                log.info("SupervisorStrategy: RESTART actorId={}", actorId);
                restart();
            }
            case STOP -> {
                log.info("SupervisorStrategy: STOP actorId={}", actorId);
                stop();
                if (actorSystem != null) {
                    actorSystem.removeActor(actorId);
                }
            }
            case ESCALATE -> {
                log.info("SupervisorStrategy: ESCALATE actorId={}", actorId);
                if (actorSystem != null) {
                    actorSystem.onEscalate(this, message, error);
                } else {
                    log.error("SupervisorStrategy: ESCALATE 但无 ActorSystem, actorId={}", actorId);
                }
            }
        }
    }

    // ==================== 数据管理 ====================

    public void markDirty() {
        dirty.set(true);
    }

    public void checkAndSave(long saveIntervalMs) {
        long now = System.currentTimeMillis();
        if (dirty.get() && (now - lastSaveTime.get()) >= saveIntervalMs) {
            try {
                saveData();
                dirty.set(false);
                lastSaveTime.set(now);
            } catch (Exception e) {
                log.error("Actor 保存数据异常: actorId={}", actorId, e);
            }
        }
    }

    // ==================== 状态查询 ====================

    public boolean isIdle(long idleTimeoutMs) {
        return (System.currentTimeMillis() - lastActiveTime.get()) > idleTimeoutMs;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public int getMailboxSize() {
        return mailbox.size();
    }

    /**
     * 获取邮箱使用率 (0.0 ~ 1.0+)
     * <p>
     * DYNAMIC_GROW 模式下可能超过 1.0
     * </p>
     */
    public float getMailboxUsage() {
        return (float) mailbox.size() / maxMailboxSize;
    }
}
