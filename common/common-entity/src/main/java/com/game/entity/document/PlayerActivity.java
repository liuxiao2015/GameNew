package com.game.entity.document;

import com.game.data.mongo.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 玩家活动数据
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "player_activity")
@CompoundIndexes({
        @CompoundIndex(name = "idx_role_activity", def = "{'roleId': 1, 'activityId': 1}", unique = true),
        @CompoundIndex(name = "idx_activity_version", def = "{'activityId': 1, 'activityVersion': 1}")
})
public class PlayerActivity extends BaseDocument {

    private long roleId;
    private int activityId;
    private int activityVersion;
    private Map<String, Long> progress = new HashMap<>();
    private Set<String> claimedRewards = new HashSet<>();
    private Map<String, Object> extraData = new HashMap<>();
    private int participateCount;
    private LocalDateTime lastParticipateTime;
    private LocalDateTime lastResetTime;
    private int todayCount;
    private String todayDate;

    public void addProgress(String goalId, long delta) {
        progress.merge(goalId, delta, Long::sum);
    }

    public void setProgress(String goalId, long value) {
        progress.put(goalId, value);
    }

    public long getProgress(String goalId) {
        return progress.getOrDefault(goalId, 0L);
    }

    public void claimReward(String rewardId) {
        claimedRewards.add(rewardId);
    }

    public boolean isRewardClaimed(String rewardId) {
        return claimedRewards.contains(rewardId);
    }

    public void setExtra(String key, Object value) {
        extraData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, T defaultValue) {
        Object value = extraData.get(key);
        return value != null ? (T) value : defaultValue;
    }

    public void resetDaily(String newDate) {
        if (!newDate.equals(this.todayDate)) {
            this.todayDate = newDate;
            this.todayCount = 0;
        }
    }

    public void reset() {
        this.progress.clear();
        this.claimedRewards.clear();
        this.extraData.clear();
        this.todayCount = 0;
        this.lastResetTime = LocalDateTime.now();
    }
}
