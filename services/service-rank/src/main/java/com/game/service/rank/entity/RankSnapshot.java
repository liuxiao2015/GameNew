package com.game.service.rank.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 排行榜快照实体 (用于历史记录)
 *
 * @author GameServer
 */
@Data
@Document(collection = "rank_snapshot")
@CompoundIndexes({
    @CompoundIndex(name = "idx_type_time", def = "{'rankType': 1, 'snapshotTime': -1}")
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
     * 快照时间
     */
    private long snapshotTime;

    /**
     * 排行数据
     */
    private List<RankEntry> entries;

    /**
     * 排行条目
     */
    @Data
    public static class RankEntry {
        private int rank;
        private long roleId;
        private String roleName;
        private int level;
        private long score;
    }
}
