package com.game.core.consistency;

import com.game.common.util.JsonUtil;
import com.game.data.redis.RedisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 补偿服务
 * <p>
 * 提供跨服务操作的补偿机制，实现最终一致性：
 * <ul>
 *     <li>记录待补偿操作</li>
 *     <li>自动重试失败操作</li>
 *     <li>支持自定义补偿逻辑</li>
 *     <li>持久化补偿记录到 Redis</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用场景：
 * - 跨服转账/交易
 * - 跨服道具发放
 * - 任何需要保证最终一致性的操作
 * </pre>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 注册补偿处理器
 * compensationService.registerHandler("guild:donate", record -> {
 *     // 补偿逻辑：回滚玩家金币
 *     playerService.addGold(record.getRoleId(), record.getData("amount"));
 * });
 *
 * // 执行可补偿操作
 * compensationService.executeWithCompensation(
 *     "guild:donate",
 *     roleId,
 *     Map.of("amount", 1000, "guildId", guildId),
 *     () -> {
 *         // 先扣玩家金币
 *         playerService.deductGold(roleId, 1000);
 *         // 再给公会加金币 (可能失败)
 *         guildService.addFund(guildId, 1000);
 *         return true;
 *     }
 * );
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final RedisService redisService;

    /**
     * 补偿记录 Key 前缀
     */
    private static final String COMPENSATION_KEY_PREFIX = "compensation:";

    /**
     * 待处理补偿集合
     */
    private static final String PENDING_SET = "compensation:pending";

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 重试间隔 (秒)
     */
    private static final int RETRY_INTERVAL_SECONDS = 60;

    /**
     * 记录过期时间
     */
    private static final Duration RECORD_EXPIRE = Duration.ofDays(7);

    /**
     * 补偿处理器注册表
     */
    private final Map<String, Consumer<CompensationRecord>> handlers = new ConcurrentHashMap<>();

    /**
     * 注册补偿处理器
     *
     * @param type    补偿类型
     * @param handler 处理器
     */
    public void registerHandler(String type, Consumer<CompensationRecord> handler) {
        handlers.put(type, handler);
        log.info("注册补偿处理器: type={}", type);
    }

    /**
     * 执行可补偿操作
     *
     * @param type        操作类型
     * @param roleId      相关角色 ID
     * @param contextData 上下文数据 (用于补偿)
     * @param action      要执行的操作
     * @param <T>         返回类型
     * @return 操作结果
     */
    public <T> T executeWithCompensation(String type, long roleId,
                                          Map<String, Object> contextData,
                                          Supplier<T> action) {
        // 创建补偿记录
        CompensationRecord record = new CompensationRecord();
        record.setRecordId(generateRecordId());
        record.setType(type);
        record.setRoleId(roleId);
        record.setContextData(contextData);
        record.setCreateTime(Instant.now());
        record.setStatus(CompensationStatus.PENDING);
        record.setRetryCount(0);
        record.setMaxRetries(DEFAULT_MAX_RETRIES);

        // 先保存补偿记录
        saveRecord(record);

        try {
            // 执行业务操作
            T result = action.get();

            // 成功后删除补偿记录
            removeRecord(record.getRecordId());

            return result;

        } catch (Exception e) {
            // 失败，标记需要补偿
            record.setStatus(CompensationStatus.FAILED);
            record.setLastError(e.getMessage());
            record.setNextRetryTime(Instant.now().plusSeconds(RETRY_INTERVAL_SECONDS));
            saveRecord(record);

            log.error("操作失败，已记录待补偿: type={}, roleId={}, recordId={}",
                    type, roleId, record.getRecordId(), e);

            throw e;
        }
    }

    /**
     * 手动触发补偿
     */
    public boolean triggerCompensation(String recordId) {
        CompensationRecord record = getRecord(recordId);
        if (record == null) {
            log.warn("补偿记录不存在: recordId={}", recordId);
            return false;
        }

        return executeCompensation(record);
    }

    /**
     * 执行补偿
     */
    private boolean executeCompensation(CompensationRecord record) {
        Consumer<CompensationRecord> handler = handlers.get(record.getType());
        if (handler == null) {
            log.error("补偿处理器不存在: type={}", record.getType());
            return false;
        }

        try {
            handler.accept(record);
            
            // 补偿成功，标记完成
            record.setStatus(CompensationStatus.COMPENSATED);
            saveRecord(record);
            
            log.info("补偿执行成功: type={}, roleId={}, recordId={}",
                    record.getType(), record.getRoleId(), record.getRecordId());
            return true;

        } catch (Exception e) {
            record.setRetryCount(record.getRetryCount() + 1);
            record.setLastError(e.getMessage());

            if (record.getRetryCount() >= record.getMaxRetries()) {
                // 达到最大重试次数，需要人工介入
                record.setStatus(CompensationStatus.MANUAL_REQUIRED);
                log.error("补偿失败，达到最大重试次数，需人工处理: type={}, roleId={}, recordId={}",
                        record.getType(), record.getRoleId(), record.getRecordId(), e);
            } else {
                // 设置下次重试时间
                record.setNextRetryTime(Instant.now().plusSeconds(
                        RETRY_INTERVAL_SECONDS * (long) Math.pow(2, record.getRetryCount())));
                log.warn("补偿失败，将稍后重试: type={}, roleId={}, recordId={}, retryCount={}",
                        record.getType(), record.getRoleId(), record.getRecordId(),
                        record.getRetryCount(), e);
            }

            saveRecord(record);
            return false;
        }
    }

    /**
     * 定时处理待补偿记录
     */
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void processPendingCompensations() {
        Set<String> pendingIds = redisService.sMembers(PENDING_SET);
        if (pendingIds == null || pendingIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        for (String recordId : pendingIds) {
            CompensationRecord record = getRecord(recordId);
            if (record == null) {
                redisService.sRemove(PENDING_SET, recordId);
                continue;
            }

            // 检查是否到达重试时间
            if (record.getStatus() == CompensationStatus.FAILED
                    && record.getNextRetryTime() != null
                    && record.getNextRetryTime().isBefore(now)) {
                executeCompensation(record);
            }
        }
    }

    /**
     * 获取需要人工处理的记录
     */
    public List<CompensationRecord> getManualRequiredRecords() {
        Set<String> pendingIds = redisService.sMembers(PENDING_SET);
        if (pendingIds == null || pendingIds.isEmpty()) {
            return List.of();
        }

        return pendingIds.stream()
                .map(this::getRecord)
                .filter(r -> r != null && r.getStatus() == CompensationStatus.MANUAL_REQUIRED)
                .toList();
    }

    // ==================== 私有方法 ====================

    private void saveRecord(CompensationRecord record) {
        String key = COMPENSATION_KEY_PREFIX + record.getRecordId();
        redisService.setObject(key, record, RECORD_EXPIRE);

        if (record.getStatus() == CompensationStatus.PENDING
                || record.getStatus() == CompensationStatus.FAILED
                || record.getStatus() == CompensationStatus.MANUAL_REQUIRED) {
            redisService.sAdd(PENDING_SET, record.getRecordId());
        } else {
            redisService.sRemove(PENDING_SET, record.getRecordId());
        }
    }

    private void removeRecord(String recordId) {
        String key = COMPENSATION_KEY_PREFIX + recordId;
        redisService.delete(key);
        redisService.sRemove(PENDING_SET, recordId);
    }

    private CompensationRecord getRecord(String recordId) {
        String key = COMPENSATION_KEY_PREFIX + recordId;
        return redisService.getObject(key, CompensationRecord.class);
    }

    private String generateRecordId() {
        return System.currentTimeMillis() + "-" + (int) (Math.random() * 100000);
    }

    // ==================== 数据类 ====================

    /**
     * 补偿记录
     */
    @Data
    public static class CompensationRecord {
        private String recordId;
        private String type;
        private long roleId;
        private Map<String, Object> contextData;
        private Instant createTime;
        private CompensationStatus status;
        private int retryCount;
        private int maxRetries;
        private String lastError;
        private Instant nextRetryTime;

        @SuppressWarnings("unchecked")
        public <T> T getData(String key) {
            return contextData != null ? (T) contextData.get(key) : null;
        }
    }

    /**
     * 补偿状态
     */
    public enum CompensationStatus {
        /**
         * 待处理
         */
        PENDING,
        /**
         * 失败待重试
         */
        FAILED,
        /**
         * 已补偿
         */
        COMPENSATED,
        /**
         * 需要人工处理
         */
        MANUAL_REQUIRED
    }
}
