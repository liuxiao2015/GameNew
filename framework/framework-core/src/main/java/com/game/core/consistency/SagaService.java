package com.game.core.consistency;

import com.game.core.trace.TraceContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Saga 模式服务
 * <p>
 * 提供轻量级 Saga 模式实现，用于跨服务操作的一致性保证：
 * <ul>
 *     <li>按顺序执行多个步骤</li>
 *     <li>任一步骤失败时，逆序执行已完成步骤的补偿</li>
 *     <li>支持异步执行</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用场景：
 * - 公会交易（扣玩家金币 -> 加公会资金 -> 记录日志）
 * - 跨服道具转移（从A服背包移除 -> 加到B服背包）
 * - 任何多步骤跨服操作
 * </pre>
 *
 * <pre>
 * 使用示例：
 * {@code
 * sagaService.begin()
 *     .step("扣除金币",
 *           () -> playerService.deductGold(roleId, 1000),
 *           () -> playerService.addGold(roleId, 1000))
 *     .step("增加公会资金",
 *           () -> guildService.addFund(guildId, 1000),
 *           () -> guildService.deductFund(guildId, 1000))
 *     .step("记录日志",
 *           () -> logService.recordDonation(roleId, guildId, 1000),
 *           () -> {}) // 日志不需要补偿
 *     .execute();
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaService {

    /**
     * 开始构建 Saga
     */
    public SagaBuilder begin() {
        return new SagaBuilder();
    }

    /**
     * Saga 构建器
     */
    public static class SagaBuilder {
        private final List<SagaStep> steps = new ArrayList<>();
        private String sagaName = "saga";

        /**
         * 设置 Saga 名称 (用于日志)
         */
        public SagaBuilder name(String name) {
            this.sagaName = name;
            return this;
        }

        /**
         * 添加步骤
         *
         * @param stepName     步骤名称
         * @param action       正向操作
         * @param compensation 补偿操作
         */
        public SagaBuilder step(String stepName, Runnable action, Runnable compensation) {
            steps.add(new SagaStep(stepName, () -> {
                action.run();
                return null;
            }, compensation));
            return this;
        }

        /**
         * 添加步骤 (带返回值)
         *
         * @param stepName     步骤名称
         * @param action       正向操作
         * @param compensation 补偿操作
         * @param <T>          返回类型
         */
        public <T> SagaBuilder step(String stepName, Supplier<T> action, Runnable compensation) {
            steps.add(new SagaStep(stepName, action, compensation));
            return this;
        }

        /**
         * 执行 Saga
         *
         * @return Saga 执行结果
         */
        public SagaResult execute() {
            String traceId = TraceContext.getTraceId();
            List<SagaStep> completedSteps = new ArrayList<>();

            log.info("[Saga:{}] 开始执行, 共{}个步骤, traceId={}",
                    sagaName, steps.size(), traceId);

            try {
                for (int i = 0; i < steps.size(); i++) {
                    SagaStep step = steps.get(i);
                    log.debug("[Saga:{}] 执行步骤 {}/{}: {}",
                            sagaName, i + 1, steps.size(), step.getName());

                    try {
                        Object result = step.getAction().get();
                        step.setResult(result);
                        completedSteps.add(step);

                    } catch (Exception e) {
                        log.error("[Saga:{}] 步骤{}失败: {}, 开始回滚",
                                sagaName, step.getName(), e.getMessage(), e);

                        // 执行补偿 (逆序)
                        compensate(completedSteps);

                        return SagaResult.fail(step.getName(), e);
                    }
                }

                log.info("[Saga:{}] 执行成功, traceId={}", sagaName, traceId);
                return SagaResult.success(steps);

            } catch (Exception e) {
                log.error("[Saga:{}] 执行异常", sagaName, e);
                compensate(completedSteps);
                return SagaResult.fail("unknown", e);
            }
        }

        /**
         * 执行补偿
         */
        private void compensate(List<SagaStep> completedSteps) {
            log.info("[Saga:{}] 开始补偿, 共{}个已完成步骤",
                    sagaName, completedSteps.size());

            // 逆序补偿
            for (int i = completedSteps.size() - 1; i >= 0; i--) {
                SagaStep step = completedSteps.get(i);
                try {
                    log.debug("[Saga:{}] 补偿步骤: {}", sagaName, step.getName());
                    step.getCompensation().run();
                } catch (Exception e) {
                    // 补偿失败需要人工介入
                    log.error("[Saga:{}] 补偿步骤{}失败，需人工处理: {}",
                            sagaName, step.getName(), e.getMessage(), e);
                }
            }

            log.info("[Saga:{}] 补偿完成", sagaName);
        }
    }

    /**
     * Saga 步骤
     */
    @Data
    public static class SagaStep {
        private final String name;
        private final Supplier<?> action;
        private final Runnable compensation;
        private Object result;

        public SagaStep(String name, Supplier<?> action, Runnable compensation) {
            this.name = name;
            this.action = action;
            this.compensation = compensation;
        }
    }

    /**
     * Saga 执行结果
     */
    @Data
    public static class SagaResult {
        private final boolean success;
        private final String failedStep;
        private final Exception exception;
        private final List<SagaStep> completedSteps;

        private SagaResult(boolean success, String failedStep, Exception exception,
                           List<SagaStep> completedSteps) {
            this.success = success;
            this.failedStep = failedStep;
            this.exception = exception;
            this.completedSteps = completedSteps;
        }

        public static SagaResult success(List<SagaStep> steps) {
            return new SagaResult(true, null, null, steps);
        }

        public static SagaResult fail(String failedStep, Exception e) {
            return new SagaResult(false, failedStep, e, null);
        }

        /**
         * 获取指定步骤的执行结果
         */
        @SuppressWarnings("unchecked")
        public <T> T getStepResult(String stepName) {
            if (completedSteps == null) {
                return null;
            }
            return completedSteps.stream()
                    .filter(s -> s.getName().equals(stepName))
                    .map(s -> (T) s.getResult())
                    .findFirst()
                    .orElse(null);
        }
    }
}
