package com.game.core.rank;

import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 排行榜服务
 * <p>
 * 基于 Redis ZSet 实现的高性能排行榜：
 * <ul>
 *     <li>支持多种排行榜类型</li>
 *     <li>支持分数更新和排名查询</li>
 *     <li>支持区间查询</li>
 *     <li>自动处理同分排名</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 更新分数
 * rankService.updateScore("combat_power", roleId, combatPower);
 *
 * // 获取排名 (从 1 开始)
 * int rank = rankService.getRank("combat_power", roleId);
 *
 * // 获取 Top 100
 * List<RankEntry> topList = rankService.getTopN("combat_power", 100);
 *
 * // 获取玩家附近排名
 * List<RankEntry> nearby = rankService.getNearby("combat_power", roleId, 5);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private final RedisService redisService;

    /**
     * 排行榜 Key 前缀
     */
    private static final String RANK_KEY_PREFIX = "rank:";

    // ==================== 分数操作 ====================

    /**
     * 更新分数
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @param score    分数
     */
    public void updateScore(String rankType, long memberId, double score) {
        String key = getRankKey(rankType);
        redisService.zAdd(key, String.valueOf(memberId), score);
    }

    /**
     * 增加分数
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @param delta    增量
     * @return 新分数
     */
    public double incrementScore(String rankType, long memberId, double delta) {
        String key = getRankKey(rankType);
        Double newScore = redisService.zIncrementScore(key, String.valueOf(memberId), delta);
        return newScore != null ? newScore : 0;
    }

    /**
     * 获取分数
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @return 分数，不存在返回 0
     */
    public double getScore(String rankType, long memberId) {
        String key = getRankKey(rankType);
        Double score = redisService.zScore(key, String.valueOf(memberId));
        return score != null ? score : 0;
    }

    /**
     * 移除成员
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     */
    public void removeMember(String rankType, long memberId) {
        String key = getRankKey(rankType);
        redisService.zRemove(key, String.valueOf(memberId));
    }

    // ==================== 排名查询 ====================

    /**
     * 获取排名 (从 1 开始，分数高的排名靠前)
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @return 排名，不存在返回 -1
     */
    public int getRank(String rankType, long memberId) {
        String key = getRankKey(rankType);
        Long rank = redisService.zReverseRank(key, String.valueOf(memberId));
        return rank != null ? rank.intValue() + 1 : -1;
    }

    /**
     * 获取排名和分数
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @return RankEntry，不存在返回 null
     */
    public RankEntry getRankEntry(String rankType, long memberId) {
        int rank = getRank(rankType, memberId);
        if (rank < 0) {
            return null;
        }
        double score = getScore(rankType, memberId);
        return new RankEntry(memberId, rank, score);
    }

    // ==================== 列表查询 ====================

    /**
     * 获取 Top N 排行
     *
     * @param rankType 排行榜类型
     * @param n        数量
     * @return 排行列表
     */
    public List<RankEntry> getTopN(String rankType, int n) {
        return getRange(rankType, 0, n - 1);
    }

    /**
     * 获取指定范围的排行
     *
     * @param rankType  排行榜类型
     * @param startRank 开始排名 (从 0 开始)
     * @param endRank   结束排名
     * @return 排行列表
     */
    public List<RankEntry> getRange(String rankType, int startRank, int endRank) {
        String key = getRankKey(rankType);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisService.zReverseRangeWithScores(key, startRank, endRank);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankEntry> result = new ArrayList<>(tuples.size());
        int rank = startRank + 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                long memberId = Long.parseLong(tuple.getValue());
                result.add(new RankEntry(memberId, rank++, tuple.getScore()));
            }
        }

        return result;
    }

    /**
     * 获取玩家附近的排名
     *
     * @param rankType 排行榜类型
     * @param memberId 成员 ID
     * @param range    上下范围
     * @return 排行列表
     */
    public List<RankEntry> getNearby(String rankType, long memberId, int range) {
        int rank = getRank(rankType, memberId);
        if (rank < 0) {
            return Collections.emptyList();
        }

        int startRank = Math.max(0, rank - 1 - range);
        int endRank = rank - 1 + range;

        return getRange(rankType, startRank, endRank);
    }

    // ==================== 统计 ====================

    /**
     * 获取排行榜大小
     */
    public long getSize(String rankType) {
        String key = getRankKey(rankType);
        Long size = redisService.zCard(key);
        return size != null ? size : 0;
    }

    /**
     * 清空排行榜
     */
    public void clear(String rankType) {
        String key = getRankKey(rankType);
        redisService.delete(key);
        log.info("清空排行榜: {}", rankType);
    }

    /**
     * 修剪排行榜 (只保留 Top N)
     */
    public void trim(String rankType, int keepCount) {
        String key = getRankKey(rankType);
        redisService.zRemoveRange(key, 0, -(keepCount + 1));
        log.info("修剪排行榜: {}, keepCount={}", rankType, keepCount);
    }

    // ==================== 辅助方法 ====================

    private String getRankKey(String rankType) {
        return RANK_KEY_PREFIX + rankType;
    }

    // ==================== 排行榜条目 ====================

    /**
     * 排行榜条目
     */
    public record RankEntry(long memberId, int rank, double score) {

        /**
         * 获取分数 (整数)
         */
        public long getScoreLong() {
            return (long) score;
        }

        /**
         * 获取分数 (整数)
         */
        public int getScoreInt() {
            return (int) score;
        }
    }

    // ==================== 预定义排行榜类型 ====================

    /**
     * 战力排行榜
     */
    public static final String RANK_COMBAT_POWER = "combat_power";

    /**
     * 等级排行榜
     */
    public static final String RANK_LEVEL = "level";

    /**
     * 充值排行榜
     */
    public static final String RANK_RECHARGE = "recharge";

    /**
     * 竞技场排行榜
     */
    public static final String RANK_ARENA = "arena";

    /**
     * 公会排行榜
     */
    public static final String RANK_GUILD = "guild";
}
