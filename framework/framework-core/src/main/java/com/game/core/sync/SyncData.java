package com.game.core.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步数据
 * <p>
 * 用于记录需要同步给客户端的数据变更
 * </p>
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncData {

    /**
     * 数据模块 (如 "player", "bag", "quest")
     */
    private String module;

    /**
     * 同步类型
     */
    private SyncType syncType;

    /**
     * 数据 Key (用于增量更新时标识具体数据)
     */
    private String dataKey;

    /**
     * 同步的数据
     */
    private Object data;

    /**
     * 变更字段 (增量更新时指定)
     */
    private Map<String, Object> changedFields;

    // ==================== 便捷构造方法 ====================

    /**
     * 创建全量同步数据
     */
    public static SyncData full(String module, Object data) {
        return SyncData.builder()
                .module(module)
                .syncType(SyncType.FULL)
                .data(data)
                .build();
    }

    /**
     * 创建增量更新数据
     */
    public static SyncData update(String module, String dataKey, Map<String, Object> changedFields) {
        return SyncData.builder()
                .module(module)
                .syncType(SyncType.UPDATE)
                .dataKey(dataKey)
                .changedFields(changedFields)
                .build();
    }

    /**
     * 创建单字段更新
     */
    public static SyncData update(String module, String field, Object value) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(field, value);
        return SyncData.builder()
                .module(module)
                .syncType(SyncType.UPDATE)
                .changedFields(fields)
                .build();
    }

    /**
     * 创建新增数据
     */
    public static SyncData add(String module, String dataKey, Object data) {
        return SyncData.builder()
                .module(module)
                .syncType(SyncType.ADD)
                .dataKey(dataKey)
                .data(data)
                .build();
    }

    /**
     * 创建删除数据
     */
    public static SyncData delete(String module, String dataKey) {
        return SyncData.builder()
                .module(module)
                .syncType(SyncType.DELETE)
                .dataKey(dataKey)
                .build();
    }
}
