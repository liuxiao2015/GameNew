package com.game.service.rank.service;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.api.rank.RankEntryDTO;
import com.game.api.rank.RankService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.JsonUtil;
import com.game.data.redis.RedisService;
import com.game.service.rank.constants.RankType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排行服务实现
 *
 * @author GameServer
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    /**
     * 排行榜最大容量
     */
    private static final int MAX_RANK_SIZE = 1000;

    private final RedisService redisService;

    @DubboReference(check = false)
    private PlayerService playerService;

    @Override
    public Result<List<RankEntryDTO>> getRankList(int rankType, int start, int count) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        if (start < 1) {
            start = 1;
        }
        if (count <= 0 || count > 100) {
            count = 50;
        }

        // 从 Redis 获取排行数据 (ZREVRANGE 按分数降序)
        Set<ZSetOperations.TypedTuple<String>> rankData = redisService.zReverseRangeWithScores(
            type.getRedisKey(), start - 1, start + count - 2);

        if (rankData == null || rankData.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<RankEntryDTO> result = new ArrayList<>(rankData.size());
        int rank = start;

        for (ZSetOperations.TypedTuple<String> tuple : rankData) {
            String roleIdStr = tuple.getValue();
            Double score = tuple.getScore();
            if (roleIdStr == null || score == null) {
                continue;
            }

            long roleId = Long.parseLong(roleIdStr);
            RankEntryDTO entry = buildRankEntry(type, roleId, rank++, score.longValue());
            result.add(entry);
        }

        return Result.success(result);
    }

    @Override
    public Result<RankEntryDTO> getMyRank(int rankType, long roleId) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        // 获取排名 (从 0 开始)
        Long rankIndex = redisService.zReverseRank(type.getRedisKey(), String.valueOf(roleId));
        if (rankIndex == null) {
            // 未上榜
            RankEntryDTO entry = new RankEntryDTO();
            entry.setRank(0);
            entry.setRoleId(roleId);
            entry.setScore(0);
            return Result.success(entry);
        }

        // 获取分数
        Double score = redisService.zScore(type.getRedisKey(), String.valueOf(roleId));

        RankEntryDTO entry = buildRankEntry(type, roleId, rankIndex.intValue() + 1,
            score != null ? score.longValue() : 0);
        return Result.success(entry);
    }

    @Override
    public Result<Void> updateScore(int rankType, long roleId, long score) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        // 更新分数
        redisService.zAdd(type.getRedisKey(), String.valueOf(roleId), score);

        // 限制排行榜大小
        trimRankList(type);

        // 更新玩家信息缓存
        updatePlayerInfoCache(type, roleId);

        log.debug("排行榜分数更新: type={}, roleId={}, score={}", type, roleId, score);
        return Result.success();
    }

    @Override
    public Result<Long> incrementScore(int rankType, long roleId, long delta) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        // 增加分数
        Double newScore = redisService.zIncrementScore(type.getRedisKey(), String.valueOf(roleId), delta);

        // 限制排行榜大小
        trimRankList(type);

        // 更新玩家信息缓存
        updatePlayerInfoCache(type, roleId);

        long score = newScore != null ? newScore.longValue() : 0;
        log.debug("排行榜分数增加: type={}, roleId={}, delta={}, newScore={}", type, roleId, delta, score);
        return Result.success(score);
    }

    @Override
    public Result<Void> refreshRank(int rankType) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        // 获取所有上榜玩家
        Set<String> members = redisService.zRange(type.getRedisKey(), 0, -1);
        if (members != null) {
            for (String roleIdStr : members) {
                try {
                    long roleId = Long.parseLong(roleIdStr);
                    updatePlayerInfoCache(type, roleId);
                } catch (NumberFormatException e) {
                    log.warn("无效的角色 ID: {}", roleIdStr);
                }
            }
        }

        log.info("排行榜刷新完成: type={}", type);
        return Result.success();
    }

    @Override
    public Result<List<RankEntryDTO>> getTopRank(String rankType, int count) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }
        return getRankList(type.getType(), 1, count);
    }

    @Override
    public Result<Long> getRank(String rankType, long entityId) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        Long rankIndex = redisService.zReverseRank(type.getRedisKey(), String.valueOf(entityId));
        if (rankIndex == null) {
            return Result.success(-1L); // 未上榜
        }
        return Result.success(rankIndex + 1);
    }

    @Override
    public Result<Void> updateScore(String rankType, long entityId, double score, String extra) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }

        // 更新分数
        redisService.zAdd(type.getRedisKey(), String.valueOf(entityId), score);

        // 存储额外信息
        if (extra != null && !extra.isEmpty()) {
            redisService.hSet(type.getInfoKey(), String.valueOf(entityId), extra);
        }

        // 限制排行榜大小
        trimRankList(type);

        log.debug("排行榜分数更新: type={}, entityId={}, score={}, extra={}", rankType, entityId, score, extra);
        return Result.success();
    }

    @Override
    public Result<Void> refreshRank(String rankType) {
        RankType type = RankType.of(rankType);
        if (type == null) {
            return Result.fail(ErrorCode.PARAM_ERROR);
        }
        return refreshRank(type.getType());
    }

    /**
     * 构建排行条目
     */
    private RankEntryDTO buildRankEntry(RankType type, long roleId, int rank, long score) {
        RankEntryDTO entry = new RankEntryDTO();
        entry.setRank(rank);
        entry.setRoleId(roleId);
        entry.setScore(score);

        // 从缓存获取玩家信息
        String infoJson = redisService.hGet(type.getInfoKey(), String.valueOf(roleId));
        if (infoJson != null) {
            try {
                Map<String, Object> info = JsonUtil.fromJson(infoJson, Map.class);
                if (info != null) {
                    entry.setRoleName((String) info.get("roleName"));
                    entry.setLevel((Integer) info.getOrDefault("level", 1));
                    entry.setAvatarId((Integer) info.getOrDefault("avatarId", 1));
                    entry.setGuildId(((Number) info.getOrDefault("guildId", 0L)).longValue());
                    entry.setGuildName((String) info.get("guildName"));
                }
            } catch (Exception e) {
                log.warn("解析排行玩家信息失败: roleId={}", roleId, e);
            }
        }

        // 如果缓存没有，实时查询
        if (entry.getRoleName() == null) {
            Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
            if (playerResult.isSuccess() && playerResult.getData() != null) {
                PlayerDTO player = playerResult.getData();
                entry.setRoleName(player.getRoleName());
                entry.setLevel(player.getLevel());
                entry.setAvatarId(player.getAvatarId());
                entry.setGuildId(player.getGuildId());
                entry.setGuildName(player.getGuildName());

                // 更新缓存
                updatePlayerInfoCache(type, roleId, player);
            }
        }

        return entry;
    }

    /**
     * 更新玩家信息缓存
     */
    private void updatePlayerInfoCache(RankType type, long roleId) {
        Result<PlayerDTO> playerResult = playerService.getPlayerInfo(roleId);
        if (playerResult.isSuccess() && playerResult.getData() != null) {
            updatePlayerInfoCache(type, roleId, playerResult.getData());
        }
    }

    /**
     * 更新玩家信息缓存
     */
    private void updatePlayerInfoCache(RankType type, long roleId, PlayerDTO player) {
        Map<String, Object> info = new HashMap<>();
        info.put("roleName", player.getRoleName());
        info.put("level", player.getLevel());
        info.put("avatarId", player.getAvatarId());
        info.put("guildId", player.getGuildId());
        info.put("guildName", player.getGuildName());

        redisService.hSet(type.getInfoKey(), String.valueOf(roleId), JsonUtil.toJson(info));
    }

    /**
     * 限制排行榜大小
     */
    private void trimRankList(RankType type) {
        Long size = redisService.zCard(type.getRedisKey());
        if (size != null && size > MAX_RANK_SIZE) {
            // 移除排名靠后的
            redisService.zRemoveRange(type.getRedisKey(), 0, size - MAX_RANK_SIZE - 1);
        }
    }
}
