package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 战斗操作指令 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleActionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型
     */
    private ActionType actionType;

    /**
     * 执行单位ID
     */
    private int unitId;

    /**
     * 技能ID (释放技能时)
     */
    private int skillId;

    /**
     * 目标单位ID列表
     */
    private List<Integer> targetIds;

    /**
     * 操作序列号 (防止重复提交)
     */
    private int sequence;

    /**
     * 客户端时间戳
     */
    private long clientTime;

    /**
     * 操作类型枚举
     */
    public enum ActionType {
        /**
         * 普通攻击
         */
        ATTACK,
        
        /**
         * 释放技能
         */
        SKILL,
        
        /**
         * 使用道具
         */
        USE_ITEM,
        
        /**
         * 跳过回合
         */
        SKIP,
        
        /**
         * 自动战斗
         */
        AUTO,
        
        /**
         * 投降/认输
         */
        SURRENDER,
        
        /**
         * 暂停
         */
        PAUSE,
        
        /**
         * 继续
         */
        RESUME
    }
}
