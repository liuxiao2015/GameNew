package com.game.core.metrics;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能指标收集服务
 * <p>
 * 轻量级的性能监控，支持：
 * <ul>
 *     <li>计数器 (Counter) - 累加统计</li>
 *     <li>计量器 (Gauge) - 当前值</li>
 *     <li>计时器 (Timer) - 耗时统计</li>
 *     <li>定时输出统计报告</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 计数器
 * metricsService.increment("protocol.login.count");
 * metricsService.increment("protocol.login.error", 1);
 *
 * // 计量器
 * metricsService.gauge("online.players", onlineCount);
 *
 * // 计时器 (自动计算耗时)
 * try (var timer = metricsService.timer("protocol.login.time")) {
 *     // 业务逻辑
 * }
 *
 * // 手动计时
 * metricsService.recordTime("db.query.time", 15);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
public class MetricsService {

    /**
     * 计数器
     */
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

    /**
     * 计量器
     */
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    /**
     * 计时器统计
     */
    private final Map<String, TimerStats> timers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("MetricsService 初始化完成");
    }

    // ==================== Counter ====================

    /**
     * 增加计数器
     */
    public void increment(String name) {
        increment(name, 1);
    }

    /**
     * 增加计数器 (指定增量)
     */
    public void increment(String name, long delta) {
        counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    /**
     * 获取计数器值
     */
    public long getCounter(String name) {
        LongAdder counter = counters.get(name);
        return counter != null ? counter.sum() : 0;
    }

    /**
     * 重置计数器
     */
    public void resetCounter(String name) {
        counters.remove(name);
    }

    // ==================== Gauge ====================

    /**
     * 设置计量器值
     */
    public void gauge(String name, long value) {
        gauges.computeIfAbsent(name, k -> new AtomicLong()).set(value);
    }

    /**
     * 增加计量器值
     */
    public void gaugeIncrement(String name, long delta) {
        gauges.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(delta);
    }

    /**
     * 获取计量器值
     */
    public long getGauge(String name) {
        AtomicLong gauge = gauges.get(name);
        return gauge != null ? gauge.get() : 0;
    }

    // ==================== Timer ====================

    /**
     * 开始计时 (返回 AutoCloseable 自动记录)
     */
    public TimerContext timer(String name) {
        return new TimerContext(name, this);
    }

    /**
     * 记录耗时
     */
    public void recordTime(String name, long millis) {
        timers.computeIfAbsent(name, k -> new TimerStats()).record(millis);
    }

    /**
     * 获取计时器统计
     */
    public TimerStats getTimerStats(String name) {
        return timers.get(name);
    }

    // ==================== 统计报告 ====================

    /**
     * 获取所有计数器
     */
    public Map<String, Long> getAllCounters() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }

    /**
     * 获取所有计量器
     */
    public Map<String, Long> getAllGauges() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        gauges.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * 定期输出统计报告 (每分钟)
     */
    @Scheduled(fixedRate = 60000)
    public void reportStats() {
        if (log.isInfoEnabled() && (!counters.isEmpty() || !gauges.isEmpty() || !timers.isEmpty())) {
            StringBuilder sb = new StringBuilder("\n========== Metrics Report ==========\n");

            if (!gauges.isEmpty()) {
                sb.append("Gauges:\n");
                gauges.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v.get()).append("\n"));
            }

            if (!counters.isEmpty()) {
                sb.append("Counters:\n");
                counters.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v.sum()).append("\n"));
            }

            if (!timers.isEmpty()) {
                sb.append("Timers:\n");
                timers.forEach((k, v) -> {
                    sb.append("  ").append(k)
                            .append(" count=").append(v.getCount())
                            .append(" avg=").append(v.getAvg()).append("ms")
                            .append(" max=").append(v.getMax()).append("ms")
                            .append(" min=").append(v.getMin()).append("ms")
                            .append("\n");
                });
            }

            sb.append("=====================================");
            log.info(sb.toString());
        }
    }

    // ==================== 计时器上下文 ====================

    /**
     * 计时器上下文
     */
    public static class TimerContext implements AutoCloseable {
        private final String name;
        private final MetricsService metricsService;
        private final long startTime;

        TimerContext(String name, MetricsService metricsService) {
            this.name = name;
            this.metricsService = metricsService;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void close() {
            long elapsed = System.currentTimeMillis() - startTime;
            metricsService.recordTime(name, elapsed);
        }

        /**
         * 获取已耗时 (不结束计时)
         */
        public long elapsed() {
            return System.currentTimeMillis() - startTime;
        }
    }

    // ==================== 计时器统计 ====================

    /**
     * 计时器统计
     */
    public static class TimerStats {
        private final LongAdder count = new LongAdder();
        private final LongAdder total = new LongAdder();
        private final AtomicLong max = new AtomicLong(0);
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

        void record(long millis) {
            count.increment();
            total.add(millis);
            updateMax(millis);
            updateMin(millis);
        }

        private void updateMax(long value) {
            long current;
            do {
                current = max.get();
                if (value <= current) return;
            } while (!max.compareAndSet(current, value));
        }

        private void updateMin(long value) {
            long current;
            do {
                current = min.get();
                if (value >= current) return;
            } while (!min.compareAndSet(current, value));
        }

        public long getCount() {
            return count.sum();
        }

        public long getTotal() {
            return total.sum();
        }

        public long getMax() {
            long m = max.get();
            return m == 0 ? 0 : m;
        }

        public long getMin() {
            long m = min.get();
            return m == Long.MAX_VALUE ? 0 : m;
        }

        public double getAvg() {
            long c = count.sum();
            return c > 0 ? (double) total.sum() / c : 0;
        }
    }
}
