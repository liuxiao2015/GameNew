package com.game.examples;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.game.core.context.RequestContext;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.rpc.RpcTemplate;

/**
 * 协议处理器开发示例
 * <p>
 * 展示如何使用框架开发业务逻辑，开发者只需关注业务本身
 * </p>
 *
 * @author GameServer
 */
@ProtocolController(moduleId = 0x02, value = "玩家模块")
public class HandlerExample extends BaseHandler {

    // 模拟注入的服务
    // @Autowired
    // private PlayerService playerService;

    /**
     * 示例1: 最简单的处理方法
     * <p>
     * 框架自动注入请求参数，自动处理异常
     * </p>
     */
    @Protocol(methodId = 0x01, desc = "获取玩家信息")
    public Object getPlayerInfo(Object request) {
        // 通过 RequestContext 获取上下文信息，无需在参数中传递
        long roleId = RequestContext.getRoleId();
        String roleName = RequestContext.getRoleName();

        // 或者使用基类的便捷方法
        roleId = getRoleId();
        
        // 查询玩家数据...
        // PlayerDTO player = playerService.getPlayer(roleId);
        
        return null; // 返回响应消息
    }

    /**
     * 示例2: 带校验的处理方法
     * <p>
     * 使用基类的校验方法，简洁明了
     * </p>
     */
    @Protocol(methodId = 0x02, desc = "修改昵称")
    public Object changeNickname(Object request) {
        String newNickname = "新昵称"; // 从 request 中获取
        
        // 校验参数
        checkNotEmpty(newNickname, ErrorCode.PARAM_ERROR);
        checkTrue(newNickname.length() <= 12, ErrorCode.PARAM_ERROR, "昵称最长12个字符");
        
        // 业务逻辑...
        
        return null;
    }

    /**
     * 示例3: 使用 BizException 快捷抛出异常
     * <p>
     * 业务逻辑中可以随时抛出异常，框架自动转换为客户端响应
     * </p>
     */
    @Protocol(methodId = 0x03, desc = "购买物品")
    public Object buyItem(Object request) {
        long roleId = getRoleId();
        int itemId = 1001;
        int count = 1;
        
        // 模拟业务逻辑
        long gold = 100;  // 从玩家数据获取
        long price = 200; // 从配置获取
        
        // 使用静态方法快捷抛出异常
        BizException.throwIf(gold < price * count, ErrorCode.GOLD_NOT_ENOUGH);
        
        // 或者使用条件判断
        if (gold < price * count) {
            throw new BizException(ErrorCode.GOLD_NOT_ENOUGH, "金币不足，需要 " + (price * count));
        }
        
        // 扣除金币、添加物品...
        
        logInfo("购买物品成功: itemId={}, count={}", itemId, count);
        
        return null;
    }

    /**
     * 示例4: 带限流的接口
     * <p>
     * 通过注解配置限流，框架自动处理
     * </p>
     */
    @Protocol(methodId = 0x04, desc = "刷新商店", rateLimit = 5)
    public Object refreshShop(Object request) {
        // 每秒最多5次请求，超过自动返回限流错误
        // 业务逻辑...
        return null;
    }

    /**
     * 示例5: 调用其他服务 (RPC)
     * <p>
     * 使用 RpcTemplate 简化 RPC 调用
     * </p>
     */
    @Protocol(methodId = 0x05, desc = "获取公会信息")
    public Object getGuildInfo(Object request) {
        long roleId = getRoleId();
        
        // 方式1: 调用并自动处理异常
        // GuildDTO guild = RpcTemplate.call(() -> guildService.getPlayerGuild(roleId));
        
        // 方式2: 调用并指定错误码
        // GuildDTO guild = RpcTemplate.call(
        //     () -> guildService.getPlayerGuild(roleId),
        //     ErrorCode.GUILD_NOT_FOUND
        // );
        
        // 方式3: 静默调用（失败返回 null）
        // GuildDTO guild = RpcTemplate.callSilent(() -> guildService.getPlayerGuild(roleId));
        
        return null;
    }

    /**
     * 示例6: 慢请求告警
     * <p>
     * 通过注解配置慢请求阈值，超过自动记录警告日志
     * </p>
     */
    @Protocol(methodId = 0x06, desc = "复杂计算", slowThreshold = 50)
    public Object complexCalculation(Object request) {
        // 如果处理时间超过 50ms，自动记录慢请求日志
        // 业务逻辑...
        return null;
    }

    /**
     * 示例7: 异步处理
     * <p>
     * 通过注解配置异步执行，不阻塞 Netty IO 线程
     * </p>
     */
    @Protocol(methodId = 0x07, desc = "异步任务", async = true)
    public Object asyncTask(Object request) {
        // 异步执行，使用虚拟线程
        // 业务逻辑...
        return null;
    }

    /**
     * 示例8: Actor 模式执行
     * <p>
     * 通过注解配置在 Actor 中执行，保证线程安全
     * </p>
     */
    @Protocol(methodId = 0x08, desc = "修改玩家数据", executeInActor = true)
    public Object modifyPlayerData(Object request) {
        // 消息会发送到玩家 Actor 中顺序执行
        // 业务逻辑...
        return null;
    }
}


/*
 * ==================== Actor 开发示例 ====================
 *
 * public class PlayerActor extends BaseActor<PlayerData> {
 *
 *     public PlayerActor(long roleId) {
 *         super(roleId, "Player", 1000);
 *     }
 *
 *     @Override
 *     protected PlayerData loadData() {
 *         return playerRepository.findById(getActorId()).orElse(null);
 *     }
 *
 *     @Override
 *     protected void saveData() {
 *         playerRepository.save(getData());
 *     }
 *
 *     // 使用 @MessageHandler 注解处理消息
 *     @MessageHandler("ADD_GOLD")
 *     public long addGold(AddGoldData data) {
 *         long newGold = getData().getGold() + data.getAmount();
 *         getData().setGold(newGold);
 *         markDirty();  // 标记数据已修改
 *         return newGold;  // 返回结果
 *     }
 *
 *     @MessageHandler("DAILY_RESET")
 *     public void onDailyReset() {
 *         // 每日重置逻辑
 *         getData().setDailyTaskCompleted(false);
 *         getData().setDungeonCount(0);
 *         markDirty();
 *     }
 * }
 *
 * // 使用示例
 * PlayerActor actor = actorSystem.getActor(roleId);
 * actor.fire("ADD_GOLD", new AddGoldData(100));  // 异步发送
 * Long newGold = actor.ask("ADD_GOLD", new AddGoldData(100)).join();  // 同步等待结果
 */


/*
 * ==================== Repository 开发示例 ====================
 *
 * @Repository
 * public class PlayerRepository extends BaseRepository<PlayerData, Long> {
 *
 *     public PlayerRepository(MongoTemplate mongoTemplate) {
 *         super(mongoTemplate, PlayerData.class);
 *     }
 *
 *     // 自定义查询方法
 *     public List<PlayerData> findByServerId(int serverId) {
 *         return findByField("serverId", serverId);
 *     }
 *
 *     public List<PlayerData> findTopPlayers(int limit) {
 *         return findAll(Sort.by(Sort.Direction.DESC, "combatPower"), limit);
 *     }
 *
 *     // 使用 QueryBuilder 复杂查询
 *     public List<PlayerData> searchPlayers(int serverId, int minLevel, int maxLevel) {
 *         Query query = QueryBuilder.create()
 *             .eq("serverId", serverId)
 *             .range("level", minLevel, maxLevel)
 *             .orderByDesc("combatPower")
 *             .limit(100)
 *             .build();
 *         return find(query);
 *     }
 * }
 */


/*
 * ==================== 热修复脚本示例 ====================
 *
 * // 通过 GM 后台注册脚本
 * HotfixScript script = new HotfixScript();
 * script.setScriptId("fix_gold_bug_001");
 * script.setScriptName("修复金币 Bug");
 * script.setEnabled(true);
 * script.setScriptContent("""
 *     // Groovy 脚本
 *     def roleId = ctx.roleId
 *     def redisService = ctx.redisService
 *     
 *     // 修复逻辑
 *     ctx.info("执行修复: roleId={}", roleId)
 *     
 *     // 返回结果
 *     return "修复完成"
 * """);
 * hotfixEngine.registerScript(script);
 *
 * // 执行脚本
 * HotfixContext ctx = hotfixEngine.createContext();
 * ctx.setRoleId(123456);
 * Object result = hotfixEngine.execute("fix_gold_bug_001", ctx);
 */
