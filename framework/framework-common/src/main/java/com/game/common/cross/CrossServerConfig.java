package com.game.common.cross;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨服配置
 * <p>
 * 纯 POJO 类，不包含 Spring 注解。
 * 在需要使用的服务中通过 @ConfigurationProperties 加载。
 * </p>
 *
 * @author GameServer
 */
@Data
public class CrossServerConfig {

    /**
     * 本服 ID
     */
    private int serverId = 1;

    /**
     * 服务器分组
     */
    private String serverGroup = "group-1";

    /**
     * 跨服聊天配置
     */
    private ChatConfig chat = new ChatConfig();

    /**
     * 跨服排行配置
     */
    private RankConfig rank = new RankConfig();

    /**
     * 跨服公会战配置
     */
    private GuildWarConfig guildWar = new GuildWarConfig();

    /**
     * 跨服服务配置
     */
    private ServicesConfig services = new ServicesConfig();

    @Data
    public static class ChatConfig {
        private boolean enabled = true;
        private boolean worldChannel = true;
        private boolean tradeChannel = true;
    }

    @Data
    public static class RankConfig {
        private boolean enabled = true;
        private List<String> types = new ArrayList<>();
    }

    @Data
    public static class GuildWarConfig {
        private boolean enabled = true;
        private int matchLevelRange = 3;
        private String signupTime;
        private String battleTime;
    }

    @Data
    public static class ServicesConfig {
        private ServiceConfig guild = new ServiceConfig();
        private ServiceConfig rank = new ServiceConfig();
        private ServiceConfig chat = new ServiceConfig();
    }

    @Data
    public static class ServiceConfig {
        private boolean enabled = true;
        private int timeout = 5000;
    }
}
