package com.game.core.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务实例信息
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 服务器 ID
     */
    private int serverId;

    /**
     * Worker ID
     */
    private int workerId;

    /**
     * 主机名
     */
    private String host;

    /**
     * IP 地址
     */
    private String ip;

    /**
     * 端口
     */
    private int port;

    /**
     * 启动时间
     */
    private long startTime;

    /**
     * 最后心跳时间
     */
    private long lastHeartbeat;

    /**
     * 状态
     */
    private String status;

    /**
     * 版本
     */
    private String version;

    /**
     * 判断是否在线 (30秒内有心跳)
     */
    public boolean isOnline() {
        return System.currentTimeMillis() - lastHeartbeat < 30000;
    }
}
