package com.game.api.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * PVP 匹配结果 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 匹配状态 (0:匹配中 1:匹配成功 2:匹配失败 3:已取消)
     */
    private int status;

    /**
     * 战斗ID (匹配成功时有值)
     */
    private long battleId;

    /**
     * 预计等待时间 (秒)
     */
    private int estimatedWaitTime;

    /**
     * 已等待时间 (秒)
     */
    private int waitedTime;

    /**
     * 队伍信息 (匹配成功时)
     */
    private List<TeamMemberDTO> teamMembers;

    /**
     * 对手信息 (匹配成功时)
     */
    private List<TeamMemberDTO> opponents;

    /**
     * 队伍成员 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private long roleId;
        private String roleName;
        private int level;
        private int avatarId;
        private int score;      // 积分/段位
        private int winRate;    // 胜率 (百分比)
    }
}
