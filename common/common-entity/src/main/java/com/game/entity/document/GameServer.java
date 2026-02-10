package com.game.entity.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 游戏服务器实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "game_server")
public class GameServer {

    /**
     * 服务器 ID
     */
    @Id
    private int serverId;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 分组 ID
     */
    private int groupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 服务器状态 (0:维护 1:流畅 2:繁忙 3:爆满 4:新服)
     */
    private int status;

    /**
     * 开服时间
     */
    private long openTime;

    /**
     * 服务器标签 (新服/推荐/火爆)
     */
    private String tag;

    /**
     * 是否推荐
     */
    private boolean recommended;

    /**
     * 最大在线人数
     */
    private int maxOnline;

    /**
     * 服务器地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private int port;

    /**
     * 是否开放
     */
    private boolean open;

    /**
     * 创建时间
     */
    private long createTime;
}
