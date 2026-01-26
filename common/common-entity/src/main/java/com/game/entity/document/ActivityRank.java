package com.game.entity.document;

import com.game.data.mongo.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 活动排行数据
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "activity_rank")
@CompoundIndexes({
        @CompoundIndex(name = "idx_activity_score", def = "{'activityId': 1, 'activityVersion': 1, 'score': -1}"),
        @CompoundIndex(name = "idx_activity_role", def = "{'activityId': 1, 'activityVersion': 1, 'roleId': 1}", unique = true)
})
public class ActivityRank extends BaseDocument {

    private int activityId;
    private int activityVersion;
    private long roleId;
    private String roleName;
    private int serverId;
    private long score;
    private int rank;
    private String extraJson;
    private LocalDateTime lastUpdateTime;
}
