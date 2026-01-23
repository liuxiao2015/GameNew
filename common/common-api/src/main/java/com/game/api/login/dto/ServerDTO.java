package com.game.api.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务器信息 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务器 ID
     */
    private int serverId;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器状态 (0:维护 1:流畅 2:繁忙 3:爆满 4:新服)
     */
    private int status;

    /**
     * 开服时间
     */
    private long openTime;

    /**
     * 服务器分组 ID
     */
    private int groupId;

    /**
     * 服务器标签 (新服/推荐/火爆)
     */
    private String tag;

    /**
     * 是否推荐
     */
    private boolean recommended;

    /**
     * 当前在线人数
     */
    private int onlineCount;

    /**
     * 服务器地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private int port;
}
