package com.game.core.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警服务
 * <p>
 * 生产级告警能力：
 * <ul>
 *     <li>异常次数统计</li>
 *     <li>告警抑制 (防止告警风暴)</li>
 *     <li>多渠道通知 (预留扩展)</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class AlertService {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${game.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${game.alert.suppress-minutes:5}")
    private int suppressMinutes;

    /**
     * 告警抑制记录 (alertKey -> 上次告警时间)
     */
    private final Map<String, Long> alertSuppress = new ConcurrentHashMap<>();

    /**
     * 告警计数
     */
    private final Map<String, AtomicInteger> alertCounts = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 告警方法 ====================

    /**
     * 发送告警
     *
     * @param level   告警级别
     * @param title   告警标题
     * @param content 告警内容
     */
    public void alert(AlertLevel level, String title, String content) {
        if (!alertEnabled) {
            return;
        }

        String alertKey = level.name() + ":" + title;

        // 告警抑制
        if (isSuppressed(alertKey)) {
            incrementCount(alertKey);
            return;
        }

        // 记录告警
        recordAlert(alertKey);

        // 输出日志
        String message = buildAlertMessage(level, title, content);
        switch (level) {
            case CRITICAL -> log.error("[告警] {}", message);
            case WARNING -> log.warn("[告警] {}", message);
            case INFO -> log.info("[告警] {}", message);
        }

        // TODO: 发送到外部告警渠道 (钉钉、飞书、企业微信等)
        // sendToWebhook(level, title, content);
    }

    /**
     * 异常告警
     */
    public void alertException(String title, Throwable e) {
        String content = String.format("异常类型: %s\n异常信息: %s\n发生位置: %s",
                e.getClass().getSimpleName(),
                e.getMessage(),
                getStackTraceLocation(e));
        alert(AlertLevel.CRITICAL, title, content);
    }

    /**
     * 性能告警
     */
    public void alertPerformance(String operation, long costMs, long threshold) {
        String content = String.format("操作: %s\n耗时: %dms\n阈值: %dms",
                operation, costMs, threshold);
        alert(AlertLevel.WARNING, "性能告警", content);
    }

    /**
     * 业务告警
     */
    public void alertBusiness(String title, String content) {
        alert(AlertLevel.WARNING, title, content);
    }

    /**
     * 信息通知
     */
    public void notify(String title, String content) {
        alert(AlertLevel.INFO, title, content);
    }

    // ==================== 辅助方法 ====================

    private boolean isSuppressed(String alertKey) {
        Long lastTime = alertSuppress.get(alertKey);
        if (lastTime == null) {
            return false;
        }
        long suppressMs = suppressMinutes * 60 * 1000L;
        return System.currentTimeMillis() - lastTime < suppressMs;
    }

    private void recordAlert(String alertKey) {
        alertSuppress.put(alertKey, System.currentTimeMillis());
        alertCounts.computeIfAbsent(alertKey, k -> new AtomicInteger(0)).set(1);
    }

    private void incrementCount(String alertKey) {
        alertCounts.computeIfAbsent(alertKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private String buildAlertMessage(AlertLevel level, String title, String content) {
        return String.format("""
                
                ========== %s ==========
                服务: %s
                时间: %s
                标题: %s
                内容: %s
                =================================""",
                level.name(),
                applicationName,
                LocalDateTime.now().format(TIME_FORMAT),
                title,
                content);
    }

    private String getStackTraceLocation(Throwable e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];
            return String.format("%s.%s(%s:%d)",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());
        }
        return "unknown";
    }

    /**
     * 获取告警统计
     */
    public Map<String, Integer> getAlertStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        alertCounts.forEach((key, count) -> stats.put(key, count.get()));
        return stats;
    }

    /**
     * 清除告警抑制
     */
    public void clearSuppression() {
        alertSuppress.clear();
        log.info("告警抑制已清除");
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        /**
         * 严重 - 需要立即处理
         */
        CRITICAL,

        /**
         * 警告 - 需要关注
         */
        WARNING,

        /**
         * 信息 - 通知
         */
        INFO
    }
}
