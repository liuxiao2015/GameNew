package com.game.entity.document;

import com.game.data.mongo.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 活动配置实体
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "activity_config")
@CompoundIndexes({
        @CompoundIndex(name = "idx_type_status", def = "{'type': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_start_end", def = "{'startTime': 1, 'endTime': 1}")
})
public class ActivityConfig extends BaseDocument {

    @Indexed(unique = true)
    private int activityId;

    private String name;
    private String description;
    private ActivityType type;
    private ActivityStatus status;
    private ResetType resetType;
    private String templateId;
    private int activityVersion;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime rewardDeadline;
    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;
    private String cronExpression;
    private Integer openServerDayStart;
    private Integer openServerDayEnd;

    private int minLevel;
    private int maxLevel;
    private int vipRequired;
    private List<Integer> serverIds;
    private List<String> channels;

    private String rewardsJson;
    private String goalsJson;
    private Map<String, Object> params;

    private String icon;
    private String banner;
    private int sortOrder;
    private boolean showOnMain;
    private boolean showRedDot;
    private List<String> tags;

    private String operator;
    private String remark;
}
