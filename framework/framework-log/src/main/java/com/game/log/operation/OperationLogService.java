package com.game.log.operation;

import com.game.common.util.IdGenerator;
import com.game.data.mongo.MongoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 操作日志服务
 * <p>
 * 异步批量写入 MongoDB，避免阻塞业务线程
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final MongoService mongoService;

    /**
     * 日志队列
     */
    private final BlockingQueue<OperationLog> logQueue = new LinkedBlockingQueue<>(100000);

    /**
     * 批量写入大小
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 写入间隔 (毫秒)
     */
    private static final long FLUSH_INTERVAL_MS = 1000;

    /**
     * 写入线程
     */
    private Thread writerThread;

    /**
     * 是否运行
     */
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        writerThread = new Thread(this::writerLoop, "operation-log-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        log.info("操作日志服务启动");
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 写入剩余日志
        flush();
        log.info("操作日志服务关闭");
    }

    /**
     * 记录日志
     */
    public void log(OperationLog operationLog) {
        if (operationLog.getLogId() == null) {
            operationLog.setLogId(String.valueOf(IdGenerator.getInstance().nextId()));
        }
        if (operationLog.getLogTime() == 0) {
            operationLog.setLogTime(System.currentTimeMillis());
        }

        if (!logQueue.offer(operationLog)) {
            log.warn("操作日志队列已满，日志被丢弃: type={}, roleId={}", 
                    operationLog.getLogType(), operationLog.getRoleId());
        }
    }

    /**
     * 记录日志 (简化版)
     */
    public void log(long roleId, String roleName, String logType, String reason) {
        OperationLog log = new OperationLog();
        log.setRoleId(roleId);
        log.setRoleName(roleName);
        log.setLogType(logType);
        log.setReason(reason);
        log(log);
    }

    /**
     * 记录日志 (带变化量)
     */
    public void log(long roleId, String roleName, String logType, String subType,
                    Object beforeValue, Object afterValue, Long changeAmount, String reason) {
        OperationLog log = new OperationLog();
        log.setRoleId(roleId);
        log.setRoleName(roleName);
        log.setLogType(logType);
        log.setSubType(subType);
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setChangeAmount(changeAmount);
        log.setReason(reason);
        log(log);
    }

    /**
     * 记录日志 (带扩展数据)
     */
    public void log(long roleId, String roleName, String logType, String reason, 
                    Map<String, Object> extra) {
        OperationLog log = new OperationLog();
        log.setRoleId(roleId);
        log.setRoleName(roleName);
        log.setLogType(logType);
        log.setReason(reason);
        log.setExtra(extra);
        log(log);
    }

    /**
     * 写入线程循环
     */
    private void writerLoop() {
        List<OperationLog> batch = new ArrayList<>(BATCH_SIZE);
        long lastFlushTime = System.currentTimeMillis();

        while (running || !logQueue.isEmpty()) {
            try {
                OperationLog log = logQueue.poll(100, TimeUnit.MILLISECONDS);
                if (log != null) {
                    batch.add(log);
                }

                long now = System.currentTimeMillis();
                boolean shouldFlush = batch.size() >= BATCH_SIZE 
                        || (now - lastFlushTime >= FLUSH_INTERVAL_MS && !batch.isEmpty());

                if (shouldFlush) {
                    writeBatch(batch);
                    batch.clear();
                    lastFlushTime = now;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("操作日志写入异常", e);
            }
        }

        // 写入剩余日志
        if (!batch.isEmpty()) {
            writeBatch(batch);
        }
    }

    /**
     * 批量写入
     */
    private void writeBatch(List<OperationLog> batch) {
        if (batch.isEmpty()) {
            return;
        }

        try {
            mongoService.insertAll(batch);
            if (log.isDebugEnabled()) {
                log.debug("批量写入操作日志: count={}", batch.size());
            }
        } catch (Exception e) {
            log.error("批量写入操作日志失败: count={}", batch.size(), e);
        }
    }

    /**
     * 强制刷新
     */
    private void flush() {
        List<OperationLog> remaining = new ArrayList<>();
        logQueue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            writeBatch(remaining);
        }
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return logQueue.size();
    }
}
