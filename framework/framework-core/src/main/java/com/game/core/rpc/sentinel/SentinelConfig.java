package com.game.core.rpc.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 服务熔断降级配置
 * <p>
 * 提供以下保护能力：
 * <ul>
 *     <li>流量控制 - 防止服务过载</li>
 *     <li>熔断降级 - 快速失败，保护下游</li>
 *     <li>系统保护 - CPU/内存保护</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${sentinel.enabled:true}")
    private boolean enabled;

    @Value("${sentinel.flow.default-qps:1000}")
    private double defaultQps;

    @Value("${sentinel.degrade.slow-ratio-threshold:0.5}")
    private double slowRatioThreshold;

    @Value("${sentinel.degrade.slow-rt-ms:500}")
    private int slowRtMs;

    @Value("${sentinel.degrade.exception-ratio:0.5}")
    private double exceptionRatio;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Sentinel 已禁用");
            return;
        }

        initFlowRules();
        initDegradeRules();
        log.info("Sentinel 熔断降级配置初始化完成");
    }

    /**
     * 初始化流控规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 玩家服务流控
        rules.add(createFlowRule("com.game.api.player.PlayerService", defaultQps));
        
        // 公会服务流控
        rules.add(createFlowRule("com.game.api.guild.GuildService", defaultQps));
        
        // 聊天服务流控（较高QPS）
        rules.add(createFlowRule("com.game.api.chat.ChatService", defaultQps * 2));
        
        // 排行服务流控
        rules.add(createFlowRule("com.game.api.rank.RankService", defaultQps));

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 流控规则已加载: {} 条", rules.size());
    }

    /**
     * 初始化熔断规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 玩家服务熔断 - 慢调用比例
        rules.add(createDegradeRule(
                "com.game.api.player.PlayerService",
                RuleConstant.DEGRADE_GRADE_RT,
                slowRtMs,
                slowRatioThreshold
        ));

        // 玩家服务熔断 - 异常比例
        rules.add(createDegradeRule(
                "com.game.api.player.PlayerService",
                RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO,
                0,
                exceptionRatio
        ));

        // 公会服务熔断
        rules.add(createDegradeRule(
                "com.game.api.guild.GuildService",
                RuleConstant.DEGRADE_GRADE_RT,
                slowRtMs,
                slowRatioThreshold
        ));

        rules.add(createDegradeRule(
                "com.game.api.guild.GuildService",
                RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO,
                0,
                exceptionRatio
        ));

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel 熔断规则已加载: {} 条", rules.size());
    }

    /**
     * 创建流控规则
     */
    private FlowRule createFlowRule(String resource, double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setLimitApp("default");
        // 直接拒绝
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        return rule;
    }

    /**
     * 创建熔断规则
     *
     * @param resource       资源名
     * @param grade          熔断策略 (RT/异常比例/异常数)
     * @param threshold      阈值
     * @param ratioThreshold 比例阈值
     */
    private DegradeRule createDegradeRule(String resource, int grade, 
                                          double threshold, double ratioThreshold) {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setGrade(grade);

        if (grade == RuleConstant.DEGRADE_GRADE_RT) {
            // 慢调用比例策略
            rule.setCount(threshold);  // 慢调用RT阈值
            rule.setSlowRatioThreshold(ratioThreshold);  // 慢调用比例阈值
        } else {
            // 异常比例策略
            rule.setCount(ratioThreshold);  // 异常比例阈值
        }

        rule.setTimeWindow(10);  // 熔断时长(秒)
        rule.setMinRequestAmount(10);  // 最小请求数
        rule.setStatIntervalMs(1000);  // 统计时长
        return rule;
    }

    /**
     * 动态添加流控规则
     */
    public void addFlowRule(String resource, double qps) {
        List<FlowRule> rules = new ArrayList<>(FlowRuleManager.getRules());
        rules.add(createFlowRule(resource, qps));
        FlowRuleManager.loadRules(rules);
        log.info("动态添加流控规则: resource={}, qps={}", resource, qps);
    }

    /**
     * 动态添加熔断规则
     */
    public void addDegradeRule(String resource, int grade, double threshold, double ratio) {
        List<DegradeRule> rules = new ArrayList<>(DegradeRuleManager.getRules());
        rules.add(createDegradeRule(resource, grade, threshold, ratio));
        DegradeRuleManager.loadRules(rules);
        log.info("动态添加熔断规则: resource={}, grade={}", resource, grade);
    }
}
