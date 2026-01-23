package com.game.api.broadcast;

/**
 * 服务广播接口
 * <p>
 * 基于 Dubbo broadcast 集群模式实现服务间广播：
 * <ul>
 *     <li>配置热更新通知</li>
 *     <li>全服公告推送</li>
 *     <li>缓存失效通知</li>
 *     <li>活动状态变更</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 调用方使用示例：
 * {@code
 * @DubboReference(
 *     version = "1.0.0",
 *     group = "GAME_SERVER",
 *     cluster = "broadcast",  // 广播模式
 *     timeout = 3000
 * )
 * private BroadcastService broadcastService;
 *
 * // 广播配置刷新
 * broadcastService.onConfigReload("item.json", System.currentTimeMillis());
 * }
 * </pre>
 *
 * <pre>
 * 服务端实现示例：
 * {@code
 * @DubboService(version = "1.0.0", group = "GAME_SERVER")
 * public class BroadcastServiceImpl implements BroadcastService {
 *     @Override
 *     public void onConfigReload(String configName, long version) {
 *         configLoader.reload(configName);
 *     }
 * }
 * }
 * </pre>
 *
 * @author GameServer
 */
public interface BroadcastService {

    /**
     * 配置热更新通知
     *
     * @param configName 配置名称
     * @param version    版本号（时间戳）
     */
    void onConfigReload(String configName, long version);

    /**
     * 缓存失效通知
     *
     * @param cacheName 缓存名称
     * @param key       缓存 Key（null 表示清空整个缓存）
     */
    void onCacheEvict(String cacheName, String key);

    /**
     * 全服公告推送
     *
     * @param noticeType 公告类型
     * @param content    公告内容
     * @param duration   显示时长（秒）
     */
    void onServerNotice(int noticeType, String content, int duration);

    /**
     * 活动状态变更
     *
     * @param activityId 活动 ID
     * @param status     状态：1=开始, 2=结束, 3=更新
     */
    void onActivityChange(int activityId, int status);

    /**
     * 全服邮件通知（通知各服务检查并发送邮件）
     *
     * @param mailId 邮件模板 ID
     */
    void onGlobalMail(long mailId);

    /**
     * 服务器维护通知
     *
     * @param maintenanceTime 维护开始时间（时间戳）
     * @param durationMinutes 维护时长（分钟）
     */
    void onMaintenanceNotice(long maintenanceTime, int durationMinutes);

    /**
     * 玩家踢出通知（跨服踢人）
     *
     * @param roleId 角色 ID
     * @param reason 踢出原因
     */
    void onKickPlayer(long roleId, String reason);

    /**
     * 封禁玩家通知
     *
     * @param roleId    角色 ID
     * @param banType   封禁类型：1=登录, 2=聊天, 3=交易
     * @param banUntil  封禁截止时间
     */
    void onBanPlayer(long roleId, int banType, long banUntil);
}
