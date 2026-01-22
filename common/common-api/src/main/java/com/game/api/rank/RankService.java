package com.game.api.rank;

import com.game.common.result.Result;

import java.util.List;

/**
 * 排行服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-rank 模块实现
 * </p>
 *
 * @author GameServer
 */
public interface RankService {

    /**
     * 获取排行榜
     *
     * @param rankType 排行类型
     * @param start    起始名次 (从 1 开始)
     * @param count    获取数量
     * @return 排行条目列表
     */
    Result<List<RankEntryDTO>> getRankList(int rankType, int start, int count);

    /**
     * 获取排行榜前 N 名
     *
     * @param rankType 排行类型 (字符串)
     * @param count    获取数量
     * @return 排行条目列表
     */
    Result<List<RankEntryDTO>> getTopRank(String rankType, int count);

    /**
     * 获取自己的排名
     *
     * @param rankType 排行类型
     * @param roleId   角色 ID
     * @return 排名信息
     */
    Result<RankEntryDTO> getMyRank(int rankType, long roleId);

    /**
     * 获取排名
     *
     * @param rankType 排行类型 (字符串)
     * @param entityId 实体 ID (玩家/公会)
     * @return 排名 (-1 表示未上榜)
     */
    Result<Long> getRank(String rankType, long entityId);

    /**
     * 更新排行分数
     *
     * @param rankType 排行类型
     * @param roleId   角色 ID
     * @param score    分数
     * @return 操作结果
     */
    Result<Void> updateScore(int rankType, long roleId, long score);

    /**
     * 更新排行分数
     *
     * @param rankType 排行类型 (字符串)
     * @param entityId 实体 ID
     * @param score    分数
     * @param extra    额外信息 (名称等)
     * @return 操作结果
     */
    Result<Void> updateScore(String rankType, long entityId, double score, String extra);

    /**
     * 增加排行分数
     *
     * @param rankType 排行类型
     * @param roleId   角色 ID
     * @param delta    增量
     * @return 新分数
     */
    Result<Long> incrementScore(int rankType, long roleId, long delta);

    /**
     * 刷新排行榜
     *
     * @param rankType 排行类型
     * @return 操作结果
     */
    Result<Void> refreshRank(int rankType);

    /**
     * 刷新排行榜
     *
     * @param rankType 排行类型 (字符串)
     * @return 操作结果
     */
    Result<Void> refreshRank(String rankType);
}
