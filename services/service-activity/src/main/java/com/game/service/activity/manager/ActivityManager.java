package com.game.service.activity.manager;

import com.game.entity.document.ActivityConfig;
import com.game.entity.document.ActivityStatus;
import com.game.entity.repository.ActivityConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动管理器
 * <p>
 * 负责活动配置的加载、缓存和状态管理
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityManager {

    private final ActivityConfigRepository configRepository;

    /**
     * 活动配置缓存 (activityId -> config)
     */
    private final Map<Integer, ActivityConfig> activityCache = new ConcurrentHashMap<>();

    /**
     * 运行中的活动列表
     */
    private final Set<Integer> runningActivities = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        loadAllActivities();
    }

    /**
     * 加载所有活动配置
     */
    public void loadAllActivities() {
        List<ActivityConfig> configs = configRepository.findAll();
        activityCache.clear();
        runningActivities.clear();

        for (ActivityConfig config : configs) {
            activityCache.put(config.getActivityId(), config);
            if (config.getStatus() == ActivityStatus.RUNNING) {
                runningActivities.add(config.getActivityId());
            }
        }

        log.info("加载活动配置完成: total={}, running={}", 
                activityCache.size(), runningActivities.size());
    }

    /**
     * 刷新单个活动配置
     */
    public void refreshActivity(int activityId) {
        ActivityConfig config = configRepository.findByActivityId(activityId).orElse(null);
        if (config != null) {
            activityCache.put(activityId, config);
            if (config.getStatus() == ActivityStatus.RUNNING) {
                runningActivities.add(activityId);
            } else {
                runningActivities.remove(activityId);
            }
            log.info("刷新活动配置: activityId={}, status={}", activityId, config.getStatus());
        } else {
            activityCache.remove(activityId);
            runningActivities.remove(activityId);
        }
    }

    /**
     * 获取活动配置
     */
    public ActivityConfig getActivity(int activityId) {
        return activityCache.get(activityId);
    }

    /**
     * 获取所有活动配置
     */
    public Collection<ActivityConfig> getAllActivities() {
        return activityCache.values();
    }

    /**
     * 获取运行中的活动
     */
    public List<ActivityConfig> getRunningActivities() {
        List<ActivityConfig> result = new ArrayList<>();
        for (Integer activityId : runningActivities) {
            ActivityConfig config = activityCache.get(activityId);
            if (config != null) {
                result.add(config);
            }
        }
        return result;
    }

    /**
     * 获取可见的活动列表 (预告、运行中、已结束待领奖)
     */
    public List<ActivityConfig> getVisibleActivities(int serverId) {
        List<ActivityConfig> result = new ArrayList<>();
        for (ActivityConfig config : activityCache.values()) {
            if (!config.getStatus().isVisible()) {
                continue;
            }
            // 检查服务器限制
            if (config.getServerIds() != null && !config.getServerIds().isEmpty()) {
                if (!config.getServerIds().contains(serverId)) {
                    continue;
                }
            }
            result.add(config);
        }
        // 按排序权重排序
        result.sort(Comparator.comparingInt(ActivityConfig::getSortOrder));
        return result;
    }

    /**
     * 启动活动
     */
    public void startActivity(int activityId) {
        ActivityConfig config = activityCache.get(activityId);
        if (config == null) {
            log.warn("活动不存在: activityId={}", activityId);
            return;
        }

        config.setStatus(ActivityStatus.RUNNING);
        configRepository.save(config);
        runningActivities.add(activityId);

        log.info("活动已启动: activityId={}, name={}", activityId, config.getName());
    }

    /**
     * 结束活动
     */
    public void endActivity(int activityId) {
        ActivityConfig config = activityCache.get(activityId);
        if (config == null) {
            return;
        }

        config.setStatus(ActivityStatus.ENDED);
        configRepository.save(config);
        runningActivities.remove(activityId);

        log.info("活动已结束: activityId={}, name={}", activityId, config.getName());
    }

    /**
     * 关闭活动
     */
    public void closeActivity(int activityId) {
        ActivityConfig config = activityCache.get(activityId);
        if (config == null) {
            return;
        }

        config.setStatus(ActivityStatus.CLOSED);
        configRepository.save(config);
        runningActivities.remove(activityId);

        log.info("活动已关闭: activityId={}, name={}", activityId, config.getName());
    }

    /**
     * 检查活动是否可参与
     */
    public boolean canParticipate(ActivityConfig config, int playerLevel, int vipLevel, int serverId) {
        if (!config.getStatus().canParticipate()) {
            return false;
        }

        // 等级检查
        if (playerLevel < config.getMinLevel()) {
            return false;
        }
        if (config.getMaxLevel() > 0 && playerLevel > config.getMaxLevel()) {
            return false;
        }

        // VIP检查
        if (vipLevel < config.getVipRequired()) {
            return false;
        }

        // 服务器检查
        if (config.getServerIds() != null && !config.getServerIds().isEmpty()) {
            if (!config.getServerIds().contains(serverId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 计算活动剩余时间 (秒)
     */
    public long getRemainSeconds(ActivityConfig config) {
        if (config.getEndTime() == null) {
            return -1; // 永久活动
        }
        long remainMillis = java.time.Duration.between(LocalDateTime.now(), config.getEndTime()).toMillis();
        return Math.max(0, remainMillis / 1000);
    }
}
