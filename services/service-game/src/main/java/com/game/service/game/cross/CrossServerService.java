package com.game.service.game.cross;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.cross.CrossServerConfig;
import com.game.common.cross.ServerInfo;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.JsonUtil;
import com.game.data.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨服通信服务
 * <p>
 * 处理跨服相关的业务逻辑，包括：
 * - 跨服玩家查询
 * - 跨服公会查询
 * - 跨服消息转发
 * - 服务器列表管理
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossServerService {

    private final CrossServerConfig config;
    private final RedisService redisService;

    /**
     * 跨服玩家服务引用
     * <p>
     * 通过 Dubbo 一致性哈希路由到玩家所在服务器
     * hash.arguments=0 表示按第一个参数 (roleId) 做哈希
     * </p>
     */
    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 3000,
        retries = 0,
        loadbalance = "consistenthash",
        parameters = {"hash.arguments", "0", "hash.nodes", "160"}
    )
    private PlayerService playerService;

    /**
     * 跨服公会服务引用
     */
    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 5000,
        retries = 0,
        loadbalance = "consistenthash",
        parameters = {"hash.arguments", "0", "hash.nodes", "160"}
    )
    private GuildService guildService;

    /**
     * 服务器信息缓存
     */
    private final Map<Integer, ServerInfo> serverCache = new ConcurrentHashMap<>();

    // ==================== Redis Keys ====================
    private static final String KEY_SERVER_LIST = "cross:servers";
    private static final String KEY_SERVER_ONLINE = "cross:online:";

    // ==================== 跨服玩家操作 ====================

    /**
     * 跨服查询玩家信息
     *
     * @param roleId 角色 ID
     * @return 玩家信息
     */
    public Result<PlayerDTO> getCrossPlayer(long roleId) {
        try {
            return playerService.getPlayerInfo(roleId);
        } catch (Exception e) {
            log.error("跨服查询玩家失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 批量跨服查询玩家
     * <p>
     * 使用 BatchRpcCaller 并行调用，避免循环调用的性能问题。
     * </p>
     *
     * @param roleIds 角色 ID 列表
     * @return 玩家信息列表
     */
    public List<PlayerDTO> getCrossPlayers(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用并行批量调用优化性能
        Map<Long, PlayerDTO> playerMap = new ConcurrentHashMap<>();
        
        roleIds.parallelStream().forEach(roleId -> {
            try {
                Result<PlayerDTO> result = playerService.getPlayerInfo(roleId);
                if (result.isSuccess() && result.getData() != null) {
                    playerMap.put(roleId, result.getData());
                }
            } catch (Exception e) {
                log.warn("批量查询玩家失败: roleId={}", roleId, e);
            }
        });
        
        // 保持原始顺序
        return roleIds.stream()
                .map(playerMap::get)
                .filter(p -> p != null)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 跨服公会操作 ====================

    /**
     * 跨服查询公会信息
     *
     * @param guildId 公会 ID
     * @return 公会信息
     */
    public Result<GuildDTO> getCrossGuild(long guildId) {
        try {
            return guildService.getGuildInfo(guildId);
        } catch (Exception e) {
            log.error("跨服查询公会失败: guildId={}", guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ==================== 服务器列表管理 ====================

    /**
     * 获取服务器列表
     *
     * @return 服务器列表
     */
    public List<ServerInfo> getServerList() {
        // 优先从缓存获取
        if (!serverCache.isEmpty()) {
            return new ArrayList<>(serverCache.values());
        }

        // 从 Redis 获取
        Map<String, String> serverMap = redisService.hGetAll(KEY_SERVER_LIST);
        List<ServerInfo> servers = new ArrayList<>();
        
        serverMap.forEach((k, v) -> {
            ServerInfo serverInfo = JsonUtil.fromJson(v, ServerInfo.class);
            if (serverInfo != null) {
                servers.add(serverInfo);
                serverCache.put(serverInfo.getServerId(), serverInfo);
            }
        });

        return servers;
    }

    /**
     * 获取服务器信息
     *
     * @param serverId 服务器 ID
     * @return 服务器信息
     */
    public ServerInfo getServerInfo(int serverId) {
        ServerInfo server = serverCache.get(serverId);
        if (server != null) {
            return server;
        }

        // 从 Redis 获取
        String json = redisService.hGet(KEY_SERVER_LIST, String.valueOf(serverId));
        if (json != null) {
            ServerInfo serverInfo = JsonUtil.fromJson(json, ServerInfo.class);
            if (serverInfo != null) {
                serverCache.put(serverId, serverInfo);
                return serverInfo;
            }
        }
        return null;
    }

    /**
     * 更新服务器在线人数
     *
     * @param serverId    服务器 ID
     * @param onlineCount 在线人数
     */
    public void updateServerOnline(int serverId, int onlineCount) {
        String key = KEY_SERVER_ONLINE + serverId;
        redisService.set(key, String.valueOf(onlineCount), 60);

        // 更新缓存
        ServerInfo server = serverCache.get(serverId);
        if (server != null) {
            server.setOnlineCount(onlineCount);
            // 计算状态
            if (onlineCount >= server.getMaxOnline()) {
                server.setStatus(3); // 爆满
            } else if (onlineCount >= server.getMaxOnline() * 0.8) {
                server.setStatus(2); // 繁忙
            } else {
                server.setStatus(1); // 正常
            }
        }
    }

    /**
     * 注册服务器
     *
     * @param serverInfo 服务器信息
     */
    public void registerServer(ServerInfo serverInfo) {
        redisService.hSetObject(KEY_SERVER_LIST, String.valueOf(serverInfo.getServerId()), serverInfo);
        serverCache.put(serverInfo.getServerId(), serverInfo);
        log.info("服务器注册成功: serverId={}, serverName={}", 
                serverInfo.getServerId(), serverInfo.getServerName());
    }

    /**
     * 获取当前服务器配置
     */
    public CrossServerConfig getConfig() {
        return config;
    }

    /**
     * 获取当前服务器 ID
     */
    public int getCurrentServerId() {
        return config.getServerId();
    }

    /**
     * 获取当前服务器分组
     */
    public String getCurrentServerGroup() {
        return config.getServerGroup();
    }

    /**
     * 判断是否为跨服请求
     *
     * @param targetServerId 目标服务器 ID
     * @return true 表示是跨服请求
     */
    public boolean isCrossServer(int targetServerId) {
        return targetServerId != config.getServerId();
    }

    /**
     * 判断是否在同一分组
     *
     * @param serverGroup 服务器分组
     * @return true 表示在同一分组
     */
    public boolean isSameGroup(String serverGroup) {
        return config.getServerGroup().equals(serverGroup);
    }
}
