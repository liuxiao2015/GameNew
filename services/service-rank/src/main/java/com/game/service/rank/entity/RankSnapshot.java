package com.game.service.rank.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜快照实体 (用于历史记录和结算)
 *
 * @author GameServer
 */
@Data
@Document(collection = "rank_snapshot")
@CompoundIndexes({
    @CompoundIndex(name = "idx_type_time", def = "{'rankType': 1, 'snapshotTime': -1}"),
    @CompoundIndex(name = "idx_type_date", def = "{'rankType': 1, 'date': -1}")
})
public class RankSnapshot {

    /**
     * 快照 ID
     */
    @Id
    private String id;

    /**
     * 排行类型
     */
    private int rankType;

    /**
     * 快照名称 (如: daily_2026-01-23, weekly_2026-W04, season_1)
     */
    @Indexed(unique = true)
    private String snapshotName;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 日期字符串 (用于查询)
     */
    private String date;

    /**
     * 排行数据
     */
    private List<RankData> rankData;

    /**
     * 排行条目
     */
    @Data
    public static class RankData {
        private int rank;
        private long roleId;
        private String roleName;
        private int level;
        private long score;
        private long guildId;
        private String guildName;
    }
}
