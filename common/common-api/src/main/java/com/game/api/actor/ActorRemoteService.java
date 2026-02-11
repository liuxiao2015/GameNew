package com.game.api.actor;

/**
 * 远程 Actor 消息投递服务 (Dubbo RPC 接口)
 * <p>
 * 允许跨 JVM 向 Actor 发送消息。
 * 发送端使用 {@code @DubboReference}，接收端使用 {@code @DubboService}。
 * </p>
 *
 * <pre>
 * 使用方式:
 * {@code
 * @DubboReference
 * private ActorRemoteService actorRemoteService;
 *
 * // 发送消息到远程 Actor
 * actorRemoteService.tell("PlayerActorSystem", 10001L, "ADD_GOLD", "{\"amount\":100}");
 * }
 * </pre>
 *
 * @author GameServer
 */
public interface ActorRemoteService {

    /**
     * 向远程 Actor 发送消息 (fire-and-forget)
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorId         目标 Actor ID
     * @param messageType     消息类型
     * @param jsonData        消息数据 (JSON 字符串, 可为 null)
     * @return 是否成功投递到目标 Actor 的邮箱
     */
    boolean tell(String actorSystemName, long actorId, String messageType, String jsonData);

    /**
     * 向远程 Actor 发送消息并等待结果 (ask 模式, 同步阻塞)
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorId         目标 Actor ID
     * @param messageType     消息类型
     * @param jsonData        消息数据 (JSON 字符串, 可为 null)
     * @param timeoutMs       超时毫秒
     * @return 结果 JSON 字符串, 或 null
     */
    String ask(String actorSystemName, long actorId, String messageType, String jsonData, long timeoutMs);

    /**
     * 检查远程 Actor 是否存在
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorId         目标 Actor ID
     * @return 是否存在
     */
    boolean hasActor(String actorSystemName, long actorId);

    /**
     * 批量向远程 Actor 发送消息 (减少 RPC 调用次数)
     * <p>
     * 所有消息发往同一个 ActorSystem，但目标 actorId 不同。
     * </p>
     *
     * @param actorSystemName 目标 ActorSystem 名称
     * @param actorIds        目标 Actor ID 列表 (逗号分隔)
     * @param messageType     消息类型 (所有 Actor 相同)
     * @param jsonData        消息数据 (所有 Actor 相同, 可为 null)
     * @return 成功投递的数量
     */
    int batchTell(String actorSystemName, String actorIds, String messageType, String jsonData);

    /**
     * 查询该节点上注册的所有 ActorSystem 名称
     *
     * @return 逗号分隔的 ActorSystem 名称列表
     */
    String getActorSystemNames();
}
