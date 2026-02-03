package com.game.gateway.service;

import com.game.api.push.PushTargetService;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推送目标服务实现
 * <p>
 * 在 Gateway 层实现，直接操作玩家 Session 进行消息推送。
 * 充分利用 Dubbo 的能力支撑客户端消息广播：
 * <ul>
 *     <li>单播使用一致性哈希路由到正确的 Gateway</li>
 *     <li>广播使用 broadcast 集群模式发送到所有 Gateway</li>
 *     <li>支持条件过滤，减少无效推送</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@DubboService(version = "1.0.0", group = "GAME_SERVER")
@RequiredArgsConstructor
public class PushTargetServiceImpl implements PushTargetService {

    private final SessionManager sessionManager;

    /**
     * 公会成员缓存: guildId -> roleIds
     * 由 GuildService 同步更新
     */
    private final Map<Long, List<Long>> guildMemberCache = new ConcurrentHashMap<>();

    /**
     * 场景玩家缓存: sceneId -> roleIds
     * 由场景系统同步更新
     */
    private final Map<Long, List<Long>> scenePlayerCache = new ConcurrentHashMap<>();

    /**
     * 房间玩家缓存: roomId -> roleIds
     * 由房间系统同步更新
     */
    private final Map<Long, List<Long>> roomPlayerCache = new ConcurrentHashMap<>();

    // ==================== 单播 ====================

    @Override
    public boolean pushToPlayer(long roleId, int protocolId, byte[] data) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session == null || !session.isActive()) {
            return false;
        }

        try {
            session.sendPush(protocolId, data);
            return true;
        } catch (Exception e) {
            log.error("推送消息失败: roleId={}, protocolId=0x{}", 
                    roleId, Integer.toHexString(protocolId), e);
            return false;
        }
    }

    // ==================== 多播 ====================

    @Override
    public int pushToPlayers(List<Long> roleIds, int protocolId, byte[] data) {
        if (roleIds == null || roleIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Long roleId : roleIds) {
            Session session = sessionManager.getSessionByRoleId(roleId);
            if (session != null && session.isActive()) {
                try {
                    session.sendPush(protocolId, data);
                    count++;
                } catch (Exception e) {
                    log.error("多播推送失败: roleId={}", roleId, e);
                }
            }
        }
        return count;
    }

    // ==================== 广播 ====================

    @Override
    public int broadcast(int protocolId, byte[] data) {
        Collection<Session> allSessions = sessionManager.getAllSessions();
        int count = 0;

        for (Session session : allSessions) {
            if (session.isActive() && session.getRoleId() > 0) {
                try {
                    session.sendPush(protocolId, data);
                    count++;
                } catch (Exception e) {
                    log.error("广播推送失败: roleId={}", session.getRoleId(), e);
                }
            }
        }

        if (count > 0) {
            log.debug("广播推送完成: protocolId=0x{}, count={}", 
                    Integer.toHexString(protocolId), count);
        }
        return count;
    }

    @Override
    public int broadcastToServer(int serverId, int protocolId, byte[] data) {
        Collection<Session> allSessions = sessionManager.getAllSessions();
        int count = 0;

        for (Session session : allSessions) {
            if (session.isActive() && session.getRoleId() > 0 
                    && session.getServerId() == serverId) {
                try {
                    session.sendPush(protocolId, data);
                    count++;
                } catch (Exception e) {
                    log.error("服务器广播推送失败: serverId={}, roleId={}", 
                            serverId, session.getRoleId(), e);
                }
            }
        }

        if (count > 0) {
            log.debug("服务器广播完成: serverId={}, protocolId=0x{}, count={}", 
                    serverId, Integer.toHexString(protocolId), count);
        }
        return count;
    }

    @Override
    public int broadcastWithFilter(int protocolId, byte[] data, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return broadcast(protocolId, data);
        }

        Collection<Session> allSessions = sessionManager.getAllSessions();
        int count = 0;

        // 解析条件
        Integer minLevel = (Integer) conditions.get("minLevel");
        Integer maxLevel = (Integer) conditions.get("maxLevel");
        Integer vipLevel = (Integer) conditions.get("vipLevel");
        Integer serverId = (Integer) conditions.get("serverId");
        Long guildId = (Long) conditions.get("guildId");

        for (Session session : allSessions) {
            if (!session.isActive() || session.getRoleId() <= 0) {
                continue;
            }

            // 服务器过滤
            if (serverId != null && session.getServerId() != serverId) {
                continue;
            }

            // 等级过滤（需要 Session 存储等级信息）
            if (minLevel != null && session.getLevel() < minLevel) {
                continue;
            }
            if (maxLevel != null && session.getLevel() > maxLevel) {
                continue;
            }

            // VIP 等级过滤
            if (vipLevel != null && session.getVipLevel() < vipLevel) {
                continue;
            }

            // 公会过滤
            if (guildId != null && session.getGuildId() != guildId) {
                continue;
            }

            try {
                session.sendPush(protocolId, data);
                count++;
            } catch (Exception e) {
                log.error("条件广播推送失败: roleId={}", session.getRoleId(), e);
            }
        }

        if (count > 0) {
            log.debug("条件广播完成: protocolId=0x{}, conditions={}, count={}", 
                    Integer.toHexString(protocolId), conditions, count);
        }
        return count;
    }

    // ==================== 公会广播 ====================

    @Override
    public int pushToGuild(long guildId, int protocolId, byte[] data) {
        return pushToGuildExclude(guildId, protocolId, data, 0);
    }

    @Override
    public int pushToGuildExclude(long guildId, int protocolId, byte[] data, long excludeRoleId) {
        Collection<Session> allSessions = sessionManager.getAllSessions();
        int count = 0;

        for (Session session : allSessions) {
            if (session.isActive() && session.getRoleId() > 0 
                    && session.getGuildId() == guildId
                    && session.getRoleId() != excludeRoleId) {
                try {
                    session.sendPush(protocolId, data);
                    count++;
                } catch (Exception e) {
                    log.error("公会推送失败: guildId={}, roleId={}", 
                            guildId, session.getRoleId(), e);
                }
            }
        }

        if (count > 0) {
            log.debug("公会推送完成: guildId={}, protocolId=0x{}, count={}", 
                    guildId, Integer.toHexString(protocolId), count);
        }
        return count;
    }

    // ==================== 场景/房间广播 ====================

    @Override
    public int pushToScene(long sceneId, int protocolId, byte[] data) {
        // 从缓存获取场景内玩家
        List<Long> playerIds = scenePlayerCache.get(sceneId);
        if (playerIds == null || playerIds.isEmpty()) {
            return 0;
        }
        return pushToPlayers(playerIds, protocolId, data);
    }

    @Override
    public int pushToRoom(long roomId, int protocolId, byte[] data) {
        // 从缓存获取房间内玩家
        List<Long> playerIds = roomPlayerCache.get(roomId);
        if (playerIds == null || playerIds.isEmpty()) {
            return 0;
        }
        return pushToPlayers(playerIds, protocolId, data);
    }

    @Override
    public int pushToNearby(long sceneId, int centerX, int centerY, int radius, 
                            int protocolId, byte[] data) {
        // AOI 推送需要结合场景系统实现
        // 这里简化为推送给场景内所有玩家
        // 实际实现应根据坐标过滤
        log.debug("AOI 推送: sceneId={}, center=({},{}), radius={}", 
                sceneId, centerX, centerY, radius);
        return pushToScene(sceneId, protocolId, data);
    }

    // ==================== 缓存管理 ====================

    /**
     * 更新公会成员缓存
     * <p>
     * 由 GuildService 在成员变更时调用
     * </p>
     */
    public void updateGuildMembers(long guildId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            guildMemberCache.remove(guildId);
        } else {
            guildMemberCache.put(guildId, memberIds);
        }
    }

    /**
     * 更新场景玩家缓存
     */
    public void updateScenePlayers(long sceneId, List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            scenePlayerCache.remove(sceneId);
        } else {
            scenePlayerCache.put(sceneId, playerIds);
        }
    }

    /**
     * 更新房间玩家缓存
     */
    public void updateRoomPlayers(long roomId, List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            roomPlayerCache.remove(roomId);
        } else {
            roomPlayerCache.put(roomId, playerIds);
        }
    }

    /**
     * 玩家加入公会时更新 Session
     */
    public void onPlayerJoinGuild(long roleId, long guildId) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session != null) {
            session.setGuildId(guildId);
        }
    }

    /**
     * 玩家离开公会时更新 Session
     */
    public void onPlayerLeaveGuild(long roleId) {
        Session session = sessionManager.getSessionByRoleId(roleId);
        if (session != null) {
            session.setGuildId(0);
        }
    }
}
