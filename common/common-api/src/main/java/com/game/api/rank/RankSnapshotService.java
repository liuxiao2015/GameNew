package com.game.api.rank;

import com.game.common.result.Result;

/**
 * 排行榜快照服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-rank 模块实现
 * </p>
 *
 * @author GameServer
 */
public interface RankSnapshotService {

    /**
     * 保存排行榜快照
     *
     * @param rankType     排行榜类型
     * @param snapshotName 快照名称
     * @return 操作结果
     */
    Result<Void> saveSnapshot(int rankType, String snapshotName);

    /**
     * 删除指定日期之前的快照
     *
     * @param date 日期 (yyyy-MM-dd 格式)
     * @return 删除的快照数量
     */
    Result<Integer> deleteSnapshotsBefore(String date);

    /**
     * 获取排行榜快照
     *
     * @param snapshotName 快照名称
     * @return 快照数据 (JSON 格式)
     */
    Result<String> getSnapshot(String snapshotName);
}
