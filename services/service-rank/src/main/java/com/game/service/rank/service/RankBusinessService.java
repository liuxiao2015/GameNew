package com.game.service.rank.service;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.core.cache.CacheService;
import com.game.core.rank.RankService;
import com.game.data.redis.RedisService;
import com.game.proto.RankEntry;
import com.game.service.rank.constants.RankType;
import com.game.service.rank.entity.RankSnapshot;
import com.game.service.rank.repository.RankSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排行榜业务服务
 * <p>
 * 提供排行榜相关的业务逻辑，演示框架各项能力的使用：
 * <ul>
 *     <li>RankService: 排行榜基础操作</li>
 *     <li>CacheService: 排行榜数据缓存</li>
 *     <li>RedisService: 排行榜刷新时间存储</li>
 *     <li>RPC: 跨服务获取玩家详细信息</li>
 *     <li>MongoDB: 排行榜快照存储</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankBusinessService {

    private final RankService rankService;
    private final CacheService cacheService;
    private final RedisService redisService;
    private final RankSnapshotRepository rankSnapshotRepository;

    @DubboReference(check = false)
    private PlayerService playerService;

    /**
     * 排行榜刷新时间缓存
     */
    private final Map<Integer, Long> refreshTimeCache = new ConcurrentHashMap<>();

    /**
     * 排行榜缓存 Key 前缀
     */
    private static final String RANK_CACHE_KEY = "rank:cache:";
    private static final String RANK_REFRESH_KEY = "rank:refresh:";

    // ==================== 排行榜查询 ====================

    /**
     * 获取排行榜列表
     */
    public List<RankEntry> getRankList(int rankType, int start, int count) {
        String rankKey = getRankKey(rankType);
        String cacheKey = RANK_CACHE_KEY + rankType + ":" + start + ":" + count;

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<RankEntry> cached = cacheService.get(cacheKey, () -> {
            // 从 RankService 获取原始排名数据
            List<RankService.RankEntry> rawEntries = 
                    rankService.getRange(rankKey, start - 1, start + count - 2);

            if (rawEntries.isEmpty()) {
                return Collections.emptyList();
            }

            // 填充玩家详细信息
            return enrichRankEntries(rawEntries, rankType);
        }, Duration.ofMinutes(1)); // 缓存 1 分钟

        return cached != null ? cached : Collections.emptyList();
    }

    /**
     * 获取我的排名
     */
    public MyRankInfo getMyRank(long roleId, int rankType) {
        String rankKey = getRankKey(rankType);
        
        int rank = rankService.getRank(rankKey, roleId);
        double score = rankService.getScore(rankKey, roleId);

        return new MyRankInfo(rank, (long) score);
    }

    /**
     * 获取排行榜刷新时间
     */
    public long getRefreshTime(int rankType) {
        Long cached = refreshTimeCache.get(rankType);
        if (cached != null) {
            return cached;
        }

        String key = RANK_REFRESH_KEY + rankType;
        String timeStr = redisService.getString(key);
        if (timeStr != null) {
            try {
                long time = Long.parseLong(timeStr);
                refreshTimeCache.put(rankType, time);
                return time;
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return 0;
    }

    // ==================== 排行榜更新 ====================

    /**
     * 更新玩家分数
     */
    public void updateScore(long roleId, int rankType, long score) {
        String rankKey = getRankKey(rankType);
        rankService.updateScore(rankKey, roleId, score);

        log.debug("更新排行榜分数: roleId={}, rankType={}, score={}", roleId, rankType, score);
    }

    /**
     * 批量更新分数 (用于定时任务刷新)
     */
    public void batchUpdateScores(int rankType, Map<Long, Long> scores) {
        String rankKey = getRankKey(rankType);
        for (Map.Entry<Long, Long> entry : scores.entrySet()) {
            rankService.updateScore(rankKey, entry.getKey(), entry.getValue());
        }

        // 更新刷新时间
        long now = System.currentTimeMillis();
        redisService.setString(RANK_REFRESH_KEY + rankType, String.valueOf(now));
        refreshTimeCache.put(rankType, now);

        log.info("批量更新排行榜: rankType={}, count={}", rankType, scores.size());
    }

    // ==================== 排行榜快照 ====================

    /**
     * 保存排行榜快照 (用于结算和历史查询)
     */
    public void saveSnapshot(int rankType, String snapshotName) {
        String rankKey = getRankKey(rankType);
        List<RankService.RankEntry> topEntries = rankService.getTopN(rankKey, 100);

        if (topEntries.isEmpty()) {
            return;
        }

        RankSnapshot snapshot = new RankSnapshot();
        snapshot.setRankType(rankType);
        snapshot.setSnapshotName(snapshotName);
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshot.setDate(LocalDate.now().toString());

        List<RankSnapshot.RankData> rankDataList = new ArrayList<>();
        for (RankService.RankEntry entry : topEntries) {
            RankSnapshot.RankData data = new RankSnapshot.RankData();
            data.setRoleId(entry.memberId());
            data.setRank(entry.rank());
            data.setScore(entry.getScoreLong());
            
            // 获取玩家信息
            Result<PlayerDTO> playerResult = playerService.getPlayerInfo(entry.memberId());
            if (playerResult.isSuccess()) {
                PlayerDTO player = playerResult.getData();
                data.setRoleName(player.getRoleName());
                data.setLevel(player.getLevel());
                data.setGuildId(player.getGuildId());
                data.setGuildName(player.getGuildName());
            }
            
            rankDataList.add(data);
        }
        snapshot.setRankData(rankDataList);

        rankSnapshotRepository.save(snapshot);

        log.info("保存排行榜快照: rankType={}, snapshotName={}, count={}", 
                rankType, snapshotName, rankDataList.size());
    }

    /**
     * 清空排行榜 (用于赛季重置)
     */
    public void clearRank(int rankType) {
        String rankKey = getRankKey(rankType);
        rankService.clear(rankKey);
        
        // 清除缓存
        refreshTimeCache.remove(rankType);

        log.info("清空排行榜: rankType={}", rankType);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取排行榜 Key
     */
    private String getRankKey(int rankType) {
        RankType type = RankType.of(rankType);
        if (type != null) {
            return type.getRedisKey();
        }
        return "rank:" + rankType;
    }

    /**
     * 填充玩家详细信息
     */
    private List<RankEntry> enrichRankEntries(List<RankService.RankEntry> rawEntries, 
                                              int rankType) {
        List<RankEntry> result = new ArrayList<>();

        for (RankService.RankEntry raw : rawEntries) {
            RankEntry.Builder builder = RankEntry.newBuilder()
                    .setRank(raw.rank())
                    .setRoleId(raw.memberId())
                    .setScore(raw.getScoreLong());

            // 获取玩家详细信息
            Result<PlayerDTO> playerResult = playerService.getPlayerInfo(raw.memberId());
            if (playerResult.isSuccess()) {
                PlayerDTO player = playerResult.getData();
                builder.setRoleName(player.getRoleName())
                        .setLevel(player.getLevel())
                        .setAvatarId(player.getAvatarId())
                        .setGuildId(player.getGuildId())
                        .setGuildName(player.getGuildName() != null ? player.getGuildName() : "");
            } else {
                builder.setRoleName("未知玩家")
                        .setLevel(0)
                        .setAvatarId(1);
            }

            result.add(builder.build());
        }

        return result;
    }

    // ==================== 数据类 ====================

    public record MyRankInfo(int rank, long score) {}
}
