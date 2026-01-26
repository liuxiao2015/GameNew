package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 活动简要信息 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private int activityId;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动类型
     */
    private int type;

    /**
     * 活动类型名称
     */
    private String typeName;

    /**
     * 活动状态
     */
    private int status;

    /**
     * 活动状态名称
     */
    private String statusName;

    /**
     * 开始时间
     */
    private long startTime;

    /**
     * 结束时间
     */
    private long endTime;

    /**
     * 活动图标
     */
    private String icon;

    /**
     * 活动横幅
     */
    private String banner;

    /**
     * 排序权重
     */
    private int sortOrder;

    /**
     * 是否有红点
     */
    private boolean hasRedDot;

    /**
     * 活动标签
     */
    private List<String> tags;

    /**
     * 剩余时间 (秒)
     */
    private long remainSeconds;
}
