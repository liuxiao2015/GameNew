package com.game.common.cross;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务器信息
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInfo {

    /**
     * 服务器 ID
     */
    private int serverId;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器分组
     */
    private String serverGroup;

    /**
     * 服务器状态 (0-维护, 1-正常, 2-繁忙, 3-爆满)
     */
    private int status;

    /**
     * 当前在线人数
     */
    private int onlineCount;

    /**
     * 最大在线人数
     */
    private int maxOnline;

    /**
     * 服务地址
     */
    private String serviceAddress;

    /**
     * 是否为新服
     */
    private boolean newServer;

    /**
     * 是否推荐
     */
    private boolean recommended;

    /**
     * 开服时间
     */
    private long openTime;

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return status > 0;
    }
}
