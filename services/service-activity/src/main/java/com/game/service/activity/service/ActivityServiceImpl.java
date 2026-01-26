package com.game.service.activity.service;

import com.game.api.activity.*;
import com.game.api.player.PlayerService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.entity.document.*;
import com.game.entity.repository.*;
import com.game.service.activity.handler.AbstractActivityHandler;
import com.game.service.activity.handler.ActivityHandler;
import com.game.service.activity.handler.ActivityHandlerFactory;
import com.game.service.activity.manager.ActivityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityManager activityManager;
    private final ActivityHandlerFactory handlerFactory;
    private final PlayerActivityRepository playerActivityRepository;
    private final ActivityRankRepository rankRepository;

    @DubboReference(check = false)
    private PlayerService playerService;

    @Override
    public Result<List<ActivityDTO>> getActivityList(long roleId, int serverId) {
        List<ActivityConfig> activities = activityManager.getVisibleActivities(serverId);
        
        List<ActivityDTO> result = activities.stream()
                .map(config -> toActivityDTO(config, roleId))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }

    @Override
    public Result<ActivityDetailDTO> getActivityDetail(long roleId, int activityId) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        PlayerActivity playerActivity = getOrCreatePlayerActivity(roleId, config);

        // 构建详情
        ActivityDetailDTO detail = new ActivityDetailDTO();
        detail.setActivity(toActivityDTO(config, roleId));
        detail.setPlayerData(toPlayerActivityDTO(playerActivity, config));
        detail.setGoals(buildGoals(config, playerActivity));
        detail.setRewards(buildRewards(config, playerActivity, handler));
        detail.setRules(handler != null ? handler.getRules(config) : config.getDescription());

        return Result.success(detail);
    }

    @Override
    public Result<PlayerActivityDTO> getPlayerProgress(long roleId, int activityId) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        PlayerActivity playerActivity = getOrCreatePlayerActivity(roleId, config);
        return Result.success(toPlayerActivityDTO(playerActivity, config));
    }

    @Override
    public Result<List<RewardDTO>> claimReward(long roleId, int activityId, String rewardId) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        if (!config.getStatus().canClaimReward()) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "活动不可领奖");
        }

        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        if (handler == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "活动处理器不存在");
        }

        PlayerActivity playerActivity = getOrCreatePlayerActivity(roleId, config);

        if (!handler.canClaimReward(config, playerActivity, rewardId)) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "奖励不可领取");
        }

        List<RewardDTO> rewards = handler.claimReward(config, playerActivity, rewardId);
        playerActivityRepository.save(playerActivity);

        // TODO: 实际发放道具
        for (RewardDTO reward : rewards) {
            log.info("发放活动奖励: roleId={}, activityId={}, type={}, itemId={}, count={}",
                    roleId, activityId, reward.getType(), reward.getItemId(), reward.getCount());
        }

        return Result.success(rewards);
    }

    @Override
    public Result<Long> updateProgress(long roleId, int activityId, String goalId, long delta) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        if (!config.getStatus().canParticipate()) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "活动不可参与");
        }

        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        if (handler == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "活动处理器不存在");
        }

        PlayerActivity playerActivity = getOrCreatePlayerActivity(roleId, config);
        long newProgress = handler.updateProgress(config, playerActivity, goalId, delta);
        playerActivityRepository.save(playerActivity);

        return Result.success(newProgress);
    }

    @Override
    public Result<Void> participate(long roleId, int activityId, String params) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        if (!config.getStatus().canParticipate()) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "活动不可参与");
        }

        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        if (handler == null) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "活动处理器不存在");
        }

        PlayerActivity playerActivity = getOrCreatePlayerActivity(roleId, config);

        Map<String, Object> paramMap = new HashMap<>();
        // TODO: 解析 params

        boolean success = handler.participate(config, playerActivity, paramMap);
        if (!success) {
            return Result.fail(ErrorCode.OPERATION_FORBIDDEN, "参与失败");
        }

        playerActivityRepository.save(playerActivity);
        return Result.success();
    }

    @Override
    public Result<ActivityRankDTO> getActivityRank(int activityId, int page, int size) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        List<ActivityRank> ranks = rankRepository.findByActivityIdAndActivityVersionOrderByScoreDesc(
                activityId, config.getActivityVersion(), PageRequest.of(page, size));

        long totalCount = rankRepository.countByActivityIdAndActivityVersion(activityId, config.getActivityVersion());

        List<ActivityRankDTO.RankEntryDTO> rankList = new ArrayList<>();
        int startRank = page * size + 1;
        for (int i = 0; i < ranks.size(); i++) {
            ActivityRank rank = ranks.get(i);
            rankList.add(ActivityRankDTO.RankEntryDTO.builder()
                    .rank(startRank + i)
                    .roleId(rank.getRoleId())
                    .roleName(rank.getRoleName())
                    .serverId(rank.getServerId())
                    .score(rank.getScore())
                    .extra(rank.getExtraJson())
                    .build());
        }

        return Result.success(ActivityRankDTO.builder()
                .activityId(activityId)
                .rankList(rankList)
                .totalCount(totalCount)
                .build());
    }

    @Override
    public Result<PlayerRankDTO> getPlayerRank(long roleId, int activityId) {
        ActivityConfig config = activityManager.getActivity(activityId);
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND, "活动不存在");
        }

        ActivityRank rank = rankRepository.findByActivityIdAndActivityVersionAndRoleId(
                activityId, config.getActivityVersion(), roleId).orElse(null);

        long totalCount = rankRepository.countByActivityIdAndActivityVersion(activityId, config.getActivityVersion());

        if (rank == null) {
            return Result.success(PlayerRankDTO.builder()
                    .activityId(activityId)
                    .rank(0)
                    .score(0)
                    .totalCount(totalCount)
                    .build());
        }

        // TODO: 计算排名和差距
        return Result.success(PlayerRankDTO.builder()
                .activityId(activityId)
                .rank(rank.getRank())
                .score(rank.getScore())
                .totalCount(totalCount)
                .build());
    }

    @Override
    public Result<Void> dailyReset() {
        log.info("活动每日重置开始");
        String today = LocalDate.now().toString();

        for (ActivityConfig config : activityManager.getRunningActivities()) {
            ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
            if (handler == null) continue;

            List<PlayerActivity> activities = playerActivityRepository.findByActivityId(config.getActivityId());
            for (PlayerActivity playerActivity : activities) {
                playerActivity.resetDaily(today);
                handler.onDailyReset(config, playerActivity);
            }
            playerActivityRepository.saveAll(activities);
        }

        log.info("活动每日重置完成");
        return Result.success();
    }

    // ==================== 私有方法 ====================

    private PlayerActivity getOrCreatePlayerActivity(long roleId, ActivityConfig config) {
        return playerActivityRepository.findByRoleIdAndActivityIdAndActivityVersion(
                roleId, config.getActivityId(), config.getActivityVersion())
                .orElseGet(() -> {
                    PlayerActivity activity = new PlayerActivity();
                    activity.setRoleId(roleId);
                    activity.setActivityId(config.getActivityId());
                    activity.setActivityVersion(config.getActivityVersion());
                    activity.setTodayDate(LocalDate.now().toString());

                    ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
                    if (handler != null) {
                        handler.onPlayerInit(config, activity);
                    }

                    return playerActivityRepository.save(activity);
                });
    }

    private ActivityDTO toActivityDTO(ActivityConfig config, long roleId) {
        PlayerActivity playerActivity = playerActivityRepository
                .findByRoleIdAndActivityIdAndActivityVersion(roleId, config.getActivityId(), config.getActivityVersion())
                .orElse(null);

        boolean hasRedDot = false;
        if (playerActivity != null && config.isShowRedDot()) {
            ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
            if (handler != null) {
                hasRedDot = handler.hasClaimableReward(config, playerActivity);
            }
        }

        return ActivityDTO.builder()
                .activityId(config.getActivityId())
                .name(config.getName())
                .description(config.getDescription())
                .type(config.getType().getCode())
                .typeName(config.getType().getName())
                .status(config.getStatus().getCode())
                .statusName(config.getStatus().getName())
                .startTime(toEpochMilli(config.getStartTime()))
                .endTime(toEpochMilli(config.getEndTime()))
                .icon(config.getIcon())
                .banner(config.getBanner())
                .sortOrder(config.getSortOrder())
                .hasRedDot(hasRedDot)
                .tags(config.getTags())
                .remainSeconds(activityManager.getRemainSeconds(config))
                .build();
    }

    private PlayerActivityDTO toPlayerActivityDTO(PlayerActivity activity, ActivityConfig config) {
        ActivityHandler handler = handlerFactory.getHandler(config.getTemplateId());
        boolean hasClaimable = handler != null && handler.hasClaimableReward(config, activity);

        return PlayerActivityDTO.builder()
                .activityId(activity.getActivityId())
                .version(activity.getActivityVersion())
                .progress(activity.getProgress())
                .claimedRewards(activity.getClaimedRewards())
                .participateCount(activity.getParticipateCount())
                .todayCount(activity.getTodayCount())
                .lastParticipateTime(toEpochMilli(activity.getLastParticipateTime()))
                .hasClaimableReward(hasClaimable)
                .build();
    }

    private List<ActivityDetailDTO.ActivityGoalDTO> buildGoals(ActivityConfig config, PlayerActivity activity) {
        // TODO: 解析 goalsJson
        return Collections.emptyList();
    }

    private List<ActivityDetailDTO.ActivityRewardDTO> buildRewards(ActivityConfig config, 
                                                                     PlayerActivity activity, 
                                                                     ActivityHandler handler) {
        // TODO: 解析 rewardsJson
        return Collections.emptyList();
    }

    private long toEpochMilli(LocalDateTime time) {
        if (time == null) return 0;
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
