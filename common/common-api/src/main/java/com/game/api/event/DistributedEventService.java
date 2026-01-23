package com.game.api.event;

/**
 * 分布式事件服务接口
 * <p>
 * 基于 Dubbo broadcast 模式替代 Redis Pub/Sub：
 * <ul>
 *     <li>配置刷新事件</li>
 *     <li>缓存失效事件</li>
 *     <li>活动状态变更事件</li>
 *     <li>玩家状态变更事件</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 调用方式：
 * {@code
 * @DubboReference(cluster = "broadcast", timeout = 3000)
 * private DistributedEventService eventService;
 *
 * // 广播事件到所有服务
 * eventService.publishConfigReload("item.json", System.currentTimeMillis());
 * }
 * </pre>
 *
 * @author GameServer
 */
public interface DistributedEventService {

    // ==================== 配置事件 ====================

    /**
     * 配置刷新事件
     *
     * @param configName 配置名称
     * @param version    版本号
     */
    void publishConfigReload(String configName, long version);

    /**
     * 缓存失效事件
     *
     * @param cacheName 缓存名称
     * @param key       缓存 Key（null 表示清空整个缓存）
     */
    void publishCacheEvict(String cacheName, String key);

    // ==================== 活动事件 ====================

    /**
     * 活动状态变更事件
     *
     * @param activityId 活动 ID
     * @param status     状态：1=开始, 2=结束, 3=更新
     * @param data       附加数据（JSON）
     */
    void publishActivityChange(int activityId, int status, String data);

    // ==================== 玩家事件 ====================

    /**
     * 玩家上线事件
     *
     * @param roleId   角色 ID
     * @param serverId 服务器 ID
     */
    void publishPlayerOnline(long roleId, int serverId);

    /**
     * 玩家下线事件
     *
     * @param roleId   角色 ID
     * @param serverId 服务器 ID
     */
    void publishPlayerOffline(long roleId, int serverId);

    /**
     * 玩家数据变更事件
     *
     * @param roleId     角色 ID
     * @param changeType 变更类型（如 "level", "vip", "guild"）
     * @param data       变更数据（JSON）
     */
    void publishPlayerChange(long roleId, String changeType, String data);

    // ==================== 公会事件 ====================

    /**
     * 公会成员变更事件
     *
     * @param guildId    公会 ID
     * @param roleId     角色 ID
     * @param changeType 变更类型：1=加入, 2=离开, 3=踢出
     */
    void publishGuildMemberChange(long guildId, long roleId, int changeType);

    /**
     * 公会解散事件
     *
     * @param guildId 公会 ID
     */
    void publishGuildDissolve(long guildId);

    // ==================== 系统事件 ====================

    /**
     * 服务器维护通知
     *
     * @param maintenanceTime 维护开始时间
     * @param durationMinutes 维护时长（分钟）
     * @param message         维护消息
     */
    void publishMaintenanceNotice(long maintenanceTime, int durationMinutes, String message);

    /**
     * 通用事件广播
     *
     * @param eventType 事件类型
     * @param eventData 事件数据（JSON）
     */
    void publishEvent(String eventType, String eventData);
}
