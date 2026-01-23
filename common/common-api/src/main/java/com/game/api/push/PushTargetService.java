package com.game.api.push;

import java.util.List;
import java.util.Map;

/**
 * 推送目标服务接口
 * <p>
 * 基于 Dubbo 实现跨服务推送，完全替代 Redis Pub/Sub：
 * <ul>
 *     <li>单播：推送到指定玩家（一致性哈希路由）</li>
 *     <li>多播：推送到指定玩家列表</li>
 *     <li>广播：推送到所有在线玩家（broadcast 集群）</li>
 *     <li>条件广播：按条件筛选推送（等级、VIP等）</li>
 *     <li>公会广播：推送到公会成员</li>
 *     <li>区服广播：推送到指定服务器玩家</li>
 * </ul>
 * </p>
 *
 * <pre>
 * Dubbo 配置示例：
 *
 * // 单播（一致性哈希，按 roleId 路由到对应 Gateway）
 * @DubboReference(
 *     loadbalance = "consistenthash",
 *     parameters = {"hash.arguments", "0"}
 * )
 * private PushTargetService pushService;
 *
 * // 广播（发送到所有 Gateway 实例）
 * @DubboReference(cluster = "broadcast")
 * private PushTargetService broadcastPushService;
 * </pre>
 *
 * @author GameServer
 */
public interface PushTargetService {

    // ==================== 单播 ====================

    /**
     * 推送消息到指定玩家
     * <p>
     * 调用方应使用一致性哈希负载均衡
     * </p>
     *
     * @param roleId     角色 ID
     * @param protocolId 协议号
     * @param data       消息数据（Protobuf 字节数组）
     * @return true 如果玩家在线并推送成功
     */
    boolean pushToPlayer(long roleId, int protocolId, byte[] data);

    // ==================== 多播 ====================

    /**
     * 推送消息到多个玩家
     * <p>
     * 调用方应使用 broadcast 集群模式
     * </p>
     *
     * @param roleIds    角色 ID 列表
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例成功推送的数量
     */
    int pushToPlayers(List<Long> roleIds, int protocolId, byte[] data);

    // ==================== 广播 ====================

    /**
     * 广播消息到本实例所有在线玩家
     * <p>
     * 调用方应使用 broadcast 集群模式，以发送到所有 Gateway
     * </p>
     *
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int broadcast(int protocolId, byte[] data);

    /**
     * 广播消息到指定服务器的在线玩家
     * <p>
     * 调用方应使用 broadcast 集群模式
     * </p>
     *
     * @param serverId   服务器 ID
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int broadcastToServer(int serverId, int protocolId, byte[] data);

    /**
     * 条件广播 - 推送到满足条件的玩家
     * <p>
     * 支持的条件：
     * <ul>
     *     <li>minLevel: 最低等级</li>
     *     <li>maxLevel: 最高等级</li>
     *     <li>vipLevel: 最低 VIP 等级</li>
     *     <li>serverId: 指定服务器</li>
     *     <li>guildId: 指定公会</li>
     *     <li>online: 在线时长（秒）</li>
     * </ul>
     * </p>
     *
     * @param protocolId 协议号
     * @param data       消息数据
     * @param conditions 过滤条件
     * @return 本实例推送的数量
     */
    int broadcastWithFilter(int protocolId, byte[] data, Map<String, Object> conditions);

    // ==================== 公会广播 ====================

    /**
     * 推送消息到公会在线成员
     * <p>
     * 调用方应使用 broadcast 集群模式
     * </p>
     *
     * @param guildId    公会 ID
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int pushToGuild(long guildId, int protocolId, byte[] data);

    /**
     * 推送消息到公会在线成员（排除指定玩家）
     *
     * @param guildId       公会 ID
     * @param protocolId    协议号
     * @param data          消息数据
     * @param excludeRoleId 排除的角色 ID（通常是发送者）
     * @return 本实例推送的数量
     */
    int pushToGuildExclude(long guildId, int protocolId, byte[] data, long excludeRoleId);

    // ==================== 场景/房间广播 ====================

    /**
     * 推送消息到场景内玩家
     * <p>
     * 用于战斗同步、场景事件等
     * </p>
     *
     * @param sceneId    场景 ID
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int pushToScene(long sceneId, int protocolId, byte[] data);

    /**
     * 推送消息到房间内玩家
     *
     * @param roomId     房间 ID
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int pushToRoom(long roomId, int protocolId, byte[] data);

    // ==================== 附近玩家广播 ====================

    /**
     * 推送消息到附近玩家（AOI）
     *
     * @param sceneId    场景 ID
     * @param centerX    中心 X 坐标
     * @param centerY    中心 Y 坐标
     * @param radius     半径
     * @param protocolId 协议号
     * @param data       消息数据
     * @return 本实例推送的数量
     */
    int pushToNearby(long sceneId, int centerX, int centerY, int radius, 
                     int protocolId, byte[] data);
}
