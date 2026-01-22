package com.game.actor.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Actor 基类
 * <p>
 * 实现无锁化并发模型：
 * - 每个 Actor 实例对应一个实体 (玩家/公会等)
 * - 内部使用单线程顺序处理消息
 * - 通过消息队列实现线程安全
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
        /**
         * 初始化
         */
        INIT,
        /**
         * 运行中
         */
        RUNNING,
        /**
         * 已停止
         */
        STOPPED
    }

    /**
     * Actor ID
     */
    @Getter
    private final long actorId;

    /**
     * Actor 类型
     */
    @Getter
    private final String actorType;

    /**
     * Actor 数据
     */
    @Getter
    protected volatile T data;

    /**
     * 消息队列
     */
    private final BlockingQueue<ActorMessage> mailbox;

    /**
     * 消息队列最大容量
     */
    private final int maxMailboxSize;

    /**
     * 运行状态
     */
    private volatile State state = State.INIT;

    /**
     * 是否正在处理中
     */
    private final AtomicBoolean processing = new AtomicBoolean(false);

    /**
     * 是否有脏数据需要保存
     */
    @Getter
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /**
     * 最后活跃时间
     */
    @Getter
    private final AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 最后保存时间
     */
    @Getter
    private final AtomicLong lastSaveTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 创建时间
     */
    @Getter
    private final long createTime = System.currentTimeMillis();

    /**
     * Actor 系统引用
     */
    private ActorSystem actorSystem;

    protected Actor(long actorId, String actorType, int maxMailboxSize) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.maxMailboxSize = maxMailboxSize;
        this.mailbox = new LinkedBlockingQueue<>(maxMailboxSize);
    }

    /**
     * 设置 Actor 系统
     */
    void setActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

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
     * Actor 停止前的清理工作
     */
    protected void onStop() {
        // 子类可覆盖
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
            // 加载数据
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
            // 处理剩余消息
            processRemainingMessages();

            // 保存数据
            if (dirty.get()) {
                saveData();
                dirty.set(false);
            }

            // 清理工作
            onStop();

            log.info("Actor 停止成功: actorId={}, type={}", actorId, actorType);

        } catch (Exception e) {
            log.error("Actor 停止异常: actorId={}", actorId, e);
        }
    }

    /**
     * 发送消息到 Actor
     */
    public boolean tell(ActorMessage message) {
        if (state != State.RUNNING) {
            log.warn("Actor 未运行，消息被丢弃: actorId={}, msgType={}", actorId, message.getType());
            return false;
        }

        if (mailbox.size() >= maxMailboxSize) {
            log.error("Actor 消息队列已满: actorId={}, size={}", actorId, mailbox.size());
            return false;
        }

        boolean offered = mailbox.offer(message);
        if (offered) {
            lastActiveTime.set(System.currentTimeMillis());
            // 触发消息处理
            tryProcess();
        }
        return offered;
    }

    /**
     * 尝试处理消息
     */
    private void tryProcess() {
        if (processing.compareAndSet(false, true)) {
            if (actorSystem != null) {
                actorSystem.execute(this);
            } else {
                // 如果没有 ActorSystem，直接运行
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
            // 如果还有消息，继续处理
            if (!mailbox.isEmpty() && state == State.RUNNING) {
                tryProcess();
            }
        }
    }

    /**
     * 处理消息 (批量处理)
     */
    private void processMessages() {
        int processedCount = 0;
        int maxBatchSize = 100; // 每次最多处理 100 条消息

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

    /**
     * 处理剩余消息
     */
    private void processRemainingMessages() {
        while (!mailbox.isEmpty()) {
            ActorMessage message = mailbox.poll();
            if (message != null) {
                handleMessageSafe(message);
            }
        }
    }

    /**
     * 安全地处理消息
     */
    private void handleMessageSafe(ActorMessage message) {
        try {
            handleMessage(message);
        } catch (Exception e) {
            log.error("Actor 处理消息异常: actorId={}, msgType={}", actorId, message.getType(), e);
        }
    }

    /**
     * 标记数据为脏
     */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * 检查并保存数据 (定期调用)
     */
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

    /**
     * 判断是否空闲
     */
    public boolean isIdle(long idleTimeoutMs) {
        return (System.currentTimeMillis() - lastActiveTime.get()) > idleTimeoutMs;
    }

    /**
     * 判断是否运行中
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * 获取消息队列大小
     */
    public int getMailboxSize() {
        return mailbox.size();
    }
}
