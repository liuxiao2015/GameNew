package com.game.examples;

/**
 * 游戏服务器框架使用指南
 * <p>
 * 本文件展示框架提供的所有底层能力，开发者只需专注于业务逻辑
 * </p>
 *
 * @author GameServer
 */
public class FrameworkUsageExample {

    /*
     * ==================== 1. 协议处理 (最常用) ====================
     *
     * 只需继承 BaseHandler，添加 @Protocol 注解，框架自动完成：
     * - 协议路由
     * - 参数解析
     * - 登录校验
     * - 异常处理
     * - 限流控制
     *
     * @ProtocolController(moduleId = 0x02, value = "玩家")
     * public class PlayerHandler extends BaseHandler {
     *
     *     @Protocol(methodId = 0x01, desc = "获取信息")
     *     public PlayerInfoResp getInfo(PlayerInfoReq req) {
     *         long roleId = getRoleId();  // 直接获取，无需传参
     *         checkRoleId(roleId);        // 便捷校验
     *         return buildResponse(roleId);
     *     }
     *
     *     @Protocol(methodId = 0x02, desc = "购买", rateLimit = 10)
     *     public BuyResp buy(BuyReq req) {
     *         checkPositive(req.getCount(), ErrorCode.PARAM_ERROR);
     *         // 业务逻辑...
     *     }
     * }
     */


    /*
     * ==================== 2. 事件系统 ====================
     *
     * 发布事件:
     *   eventBus.publish(new PlayerLevelUpEvent(roleId, oldLevel, newLevel));
     *
     * 监听事件 (自动注册):
     *
     * @Component
     * public class RewardService {
     *     @EventListener
     *     public void onLevelUp(PlayerLevelUpEvent event) {
     *         sendLevelUpReward(event.getRoleId(), event.getNewLevel());
     *     }
     *
     *     @EventListener(priority = 100, async = true)
     *     public void logLevelUp(PlayerLevelUpEvent event) {
     *         // 高优先级异步执行
     *     }
     * }
     */


    /*
     * ==================== 3. 分布式锁 ====================
     *
     * 最简使用 (推荐):
     *   lockService.executeWithLock("order:" + orderId, () -> {
     *       return createOrder();
     *   });
     *
     * 带返回值:
     *   Order order = lockService.executeWithLock("order:" + orderId, () -> {
     *       return createOrder();
     *   });
     *
     * 手动控制:
     *   try (var lock = lockService.tryLock("trade:" + roleId)) {
     *       if (lock.isLocked()) {
     *           doTrade();
     *       }
     *   }
     */


    /*
     * ==================== 4. 限流 ====================
     *
     * 玩家操作限流:
     *   if (!rateLimiter.tryPlayerLimit(roleId, "chat", 5, Duration.ofSeconds(1))) {
     *       throw BizException.of(ErrorCode.RATE_LIMIT);
     *   }
     *
     * IP 限流:
     *   rateLimiter.tryIpLimit(ip, "login", 10, Duration.ofMinutes(1));
     *
     * 本地令牌桶:
     *   rateLimiter.tryAcquire("api:send_sms", 100);  // 每秒100个
     */


    /*
     * ==================== 5. 排行榜 ====================
     *
     * 更新分数:
     *   rankService.updateScore("combat_power", roleId, newPower);
     *
     * 获取排名:
     *   int rank = rankService.getRank("combat_power", roleId);
     *
     * 获取 Top 100:
     *   List<RankEntry> top = rankService.getTopN("combat_power", 100);
     *
     * 获取附近玩家:
     *   List<RankEntry> nearby = rankService.getNearby("combat_power", roleId, 5);
     */


    /*
     * ==================== 6. 缓存 ====================
     *
     * 自动加载:
     *   PlayerConfig config = cacheService.get("config", playerId,
     *       id -> configRepo.findById(id), PlayerConfig.class);
     *
     * 手动设置:
     *   cacheService.put("config", playerId, config);
     *
     * 删除:
     *   cacheService.evict("config", playerId);
     */


    /*
     * ==================== 7. 推送消息 ====================
     *
     * 单人推送:
     *   pushService.push(roleId, 0x0201, protoMessage);
     *
     * 多人推送:
     *   pushService.push(roleIds, 0x0301, protoMessage);
     *
     * 全服广播:
     *   pushService.broadcast(0x0101, noticeMessage);
     *
     * 条件推送:
     *   pushService.pushIf(s -> s.getLevel() > 50, 0x0401, activityMessage);
     */


    /*
     * ==================== 8. 定时器 ====================
     *
     * Buff 过期:
     *   timerService.schedule(GameTimer.builder("buff:" + buffId)
     *       .roleId(roleId)
     *       .delay(30, TimeUnit.SECONDS)
     *       .callback(t -> expireBuff(buffId))
     *       .build());
     *
     * 周期任务:
     *   timerService.schedule(GameTimer.builder("recover:" + roleId)
     *       .roleId(roleId)
     *       .period(10, TimeUnit.SECONDS)
     *       .maxExecuteCount(6)
     *       .callback(t -> recoverEnergy())
     *       .build());
     *
     * 取消:
     *   timerService.cancel("buff:" + buffId);
     *   timerService.cancelByRoleId(roleId);  // 玩家下线时
     */


    /*
     * ==================== 9. 配置加载 ====================
     *
     * 自动加载 (启动时):
     *
     * @ConfigContainer(file = "item.json", configClass = ItemConfig.class)
     * @Component
     * public class ItemConfigContainer extends BaseConfigContainer<ItemConfig> {
     * }
     *
     * 使用:
     *   ItemConfig config = itemConfigContainer.get(itemId);
     *   List<ItemConfig> all = itemConfigContainer.getAll();
     *
     * 热更新 (会广播到集群):
     *   configLoader.reloadAndBroadcast("item.json", "admin");
     */


    /*
     * ==================== 10. 性能监控 ====================
     *
     * 计数器:
     *   metrics.increment("login.success");
     *   metrics.increment("login.fail", 1);
     *
     * 计量器:
     *   metrics.gauge("online.players", onlineCount);
     *
     * 计时器:
     *   try (var t = metrics.timer("db.query")) {
     *       queryDatabase();
     *   }
     *
     * 手动记录:
     *   metrics.recordTime("rpc.call", 15);
     */


    /*
     * ==================== 11. 异步执行 ====================
     *
     * 简单异步:
     *   asyncService.run(() -> sendEmail());
     *
     * 带返回值:
     *   CompletableFuture<Data> future = asyncService.supply(() -> loadData());
     *
     * 带超时:
     *   asyncService.supplyWithTimeout(() -> callApi(), 5, TimeUnit.SECONDS);
     *
     * 并行执行:
     *   asyncService.runAll(task1, task2, task3).join();
     */


    /*
     * ==================== 12. 工具类 ====================
     *
     * 随机:
     *   RandomUtil.hit(5000);                    // 50% 命中
     *   RandomUtil.randomByWeight(items, Item::getWeight);
     *   RandomUtil.randomOne(list);
     *
     * 时间:
     *   TimeUtil.isSameDay(time1, time2);
     *   TimeUtil.getTodayStart();
     *   TimeUtil.format(timestamp);
     *
     * 数学:
     *   MathUtil.safeAdd(a, b);                  // 防溢出
     *   MathUtil.percentRate(base, 1500);        // 15%
     *   MathUtil.calcDamage(attack, defense);
     *
     * 加密:
     *   CryptoUtil.md5(password);
     *   CryptoUtil.aesEncrypt(data, key);
     *   CryptoUtil.signParams(params, secret);
     *
     * 重试:
     *   RetryUtil.retry(() -> callRemoteApi(), 3, 100);
     */


    /*
     * ==================== 13. RPC 调用 ====================
     *
     * 基本调用:
     *   GuildDTO guild = RpcTemplate.call(() -> guildService.getGuild(guildId));
     *
     * 静默调用 (失败返回 null):
     *   GuildDTO guild = RpcTemplate.callSilent(() -> guildService.getGuild(guildId));
     *
     * 指定错误码:
     *   GuildDTO guild = RpcTemplate.call(
     *       () -> guildService.getGuild(guildId),
     *       ErrorCode.GUILD_NOT_FOUND);
     */


    /*
     * ==================== 14. 数据同步 ====================
     *
     * 记录变更:
     *   dataSyncService.markFieldChanged(roleId, "player", "gold", newGold);
     *   dataSyncService.markChanged(roleId, SyncData.update("player", "level", newLevel));
     *
     * 请求结束时自动推送:
     *   dataSyncService.flush(roleId);  // 在拦截器中调用
     *
     * 立即推送:
     *   dataSyncService.syncNow(roleId, SyncData.full("bag", bagData));
     */


    /*
     * ==================== 15. ID 生成 ====================
     *
     * 通用 ID:
     *   long id = idService.nextId();
     *
     * 带类型前缀:
     *   long playerId = idService.nextPlayerId();
     *   long orderId = idService.nextOrderId();
     *   long mailId = idService.nextMailId();
     *
     * 字符串 ID:
     *   String id = idService.nextStringId("ORDER");  // ORDER_A1B2C3
     */


    /*
     * ==================== 16. 参数验证 ====================
     *
     * 静态验证:
     *   Validator.notNull(player, ErrorCode.ROLE_NOT_FOUND);
     *   Validator.notEmpty(name, ErrorCode.PARAM_ERROR, "名称不能为空");
     *   Validator.positive(gold, ErrorCode.PARAM_ERROR);
     *   Validator.range(level, 1, 100, ErrorCode.PARAM_ERROR);
     *
     * 链式验证 (Protobuf):
     *   Validator.of(request)
     *       .fieldNotEmpty("roleName", ErrorCode.PARAM_ERROR)
     *       .range("level", 1, 100, ErrorCode.PARAM_ERROR)
     *       .positive("gold", ErrorCode.PARAM_ERROR)
     *       .validate();
     */


    /*
     * ==================== 17. 链路追踪 ====================
     *
     * 自动集成 (无需手动调用):
     *   - 每个请求自动生成 traceId
     *   - 日志自动携带 traceId 和 roleId
     *
     * Logback 配置:
     *   %d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] [%X{roleId}] %-5level - %msg%n
     *
     * 手动使用:
     *   String traceId = TraceContext.getTraceId();
     *   TraceContext.setRoleId(roleId);
     */


    /*
     * ==================== 18. 优雅停机 ====================
     *
     * 实现 ShutdownAware 接口:
     *
     * @Component
     * public class PlayerService implements ShutdownAware {
     *     @Override
     *     public int getShutdownOrder() {
     *         return 500;  // 数字越大越先执行
     *     }
     *
     *     @Override
     *     public void onShutdown() {
     *         // 保存所有在线玩家数据
     *         saveAllPlayers();
     *     }
     * }
     *
     * 停机顺序建议:
     *   1000+ : 网关层 (停止接收新连接)
     *   500-999 : 业务层 (保存玩家数据)
     *   100-499 : 框架层 (停止定时任务)
     *   0-99 : 基础设施层 (关闭数据库连接)
     */


    /*
     * ==================== 19. Service 基类 ====================
     *
     * 继承 BaseService 获得便捷能力:
     *
     * @Service
     * public class RewardService extends BaseService {
     *
     *     public void sendReward(long roleId, int rewardId) {
     *         checkPositive(roleId, ErrorCode.PARAM_ERROR);
     *
     *         // 业务逻辑...
     *
     *         // 发布事件
     *         publishEvent(new RewardSentEvent(roleId, rewardId));
     *
     *         // 推送消息
     *         push(roleId, 0x0201, buildRewardMessage());
     *
     *         // 日志 (自动带 roleId)
     *         logInfo("发放奖励: rewardId={}", rewardId);
     *     }
     * }
     */


    /*
     * ==================== 19. 安全防护 ====================
     *
     * IP 黑名单:
     *   if (securityFilter.isIpBlocked(ip)) {
     *       throw new BizException(ErrorCode.IP_BLOCKED);
     *   }
     *
     * XSS 过滤:
     *   String safeContent = securityFilter.filterXss(userInput);
     *
     * 敏感词过滤:
     *   String filtered = securityFilter.filterSensitiveWords(chatContent);
     *
     * 昵称验证:
     *   if (!securityFilter.validateNickname(nickname)) {
     *       throw new BizException(ErrorCode.INVALID_NICKNAME);
     *   }
     */


    /*
     * ==================== 20. 请求验证 ====================
     *
     * 时间戳 + 签名验证 (防重放攻击):
     *   RequestValidator.ValidationResult result = requestValidator.validate(
     *       timestamp, signature, requestBody);
     *
     *   if (!result.success()) {
     *       throw new BizException(ErrorCode.INVALID_REQUEST, result.reason());
     *   }
     */


    /*
     * ==================== 21. 健康检查端点 ====================
     *
     * K8s 探针:
     *   GET /health/live   -> 存活探针 (200 = 存活)
     *   GET /health/ready  -> 就绪探针 (200 = 就绪)
     *
     * 详细信息:
     *   GET /health        -> 详细健康信息 (CPU/内存/状态)
     *   GET /health/stats  -> 服务器统计 (在线人数/请求量)
     */


    /*
     * ==================== 22. 告警通知 ====================
     *
     * 异常告警:
     *   alertService.alertException("订单创建失败", exception);
     *
     * 性能告警:
     *   alertService.alertPerformance("createOrder", costMs, threshold);
     *
     * 业务告警:
     *   alertService.alertBusiness("充值异常", "用户 " + roleId + " 充值金额异常");
     *
     * 自定义通知:
     *   alertService.notify("运维", "服务器维护通知", "预计维护2小时");
     */


    /*
     * ==================== 23. 超时控制 ====================
     *
     * 带超时执行:
     *   try {
     *       Object result = TimeoutInterceptor.executeWithTimeout(
     *           () -> callSlowApi(),
     *           5000  // 5秒超时
     *       );
     *   } catch (TimeoutException e) {
     *       log.warn("操作超时");
     *   }
     */


    /*
     * ==================== 24. 敏感数据脱敏 ====================
     *
     * 日志脱敏:
     *   String maskedPhone = SensitiveDataMasker.maskPhone("13812345678");
     *   // 输出: 138****5678
     *
     *   String maskedIdCard = SensitiveDataMasker.maskIdCard("110101199001011234");
     *   // 输出: 110101****1234
     *
     *   String maskedEmail = SensitiveDataMasker.maskEmail("test@example.com");
     *   // 输出: te***@example.com
     */


    /*
     * ==================== 25. 协议号常量 ====================
     *
     * 使用 MethodId 常量替代硬编码:
     *
     * @ProtocolController(moduleId = ProtocolConstants.PROTOCOL_PLAYER, value = "玩家")
     * public class PlayerHandler {
     *
     *     @Protocol(methodId = MethodId.Player.GET_INFO, desc = "获取信息")
     *     public Message getInfo(Session session, C2S_GetPlayerInfo req) {
     *         // 使用常量而非硬编码 methodId = 0x01
     *     }
     *
     *     @Protocol(methodId = MethodId.Player.CHANGE_NAME, desc = "改名")
     *     public Message changeName(Session session, C2S_ChangeName req) {
     *         // 使用常量而非硬编码 methodId = 0x03
     *     }
     * }
     */


    /*
     * ==================== 框架设计原则 ====================
     *
     * 1. 约定优于配置 - 遵循命名规范即可，无需大量配置
     * 2. 注解驱动 - @Protocol、@EventListener 自动注册
     * 3. 便捷方法 - 常用操作都有简化方法
     * 4. 统一异常 - BizException 自动转换为客户端响应
     * 5. 上下文传递 - RequestContext 无需参数传递
     * 6. 多机部署 - 天然支持分布式，无需额外配置
     * 7. 优雅停机 - 实现 ShutdownAware 接口自动处理
     * 8. 链路追踪 - 自动生成 traceId，日志自动携带
     * 9. 安全防护 - 内置 XSS/敏感词/IP黑名单过滤
     * 10. 生产就绪 - 健康检查、监控告警、优雅停机
     */
}
