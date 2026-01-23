# 游戏服务器框架能力总览

> **版本**: v1.0  
> **更新日期**: 2026-01  
> **设计理念**: 开发者只需关注业务逻辑，框架处理一切底层细节

---

## 目录

1. [框架核心能力](#1-框架核心能力)
2. [协议处理](#2-协议处理)
3. [数据访问](#3-数据访问)
4. [缓存系统](#4-缓存系统)
5. [分布式能力](#5-分布式能力)
6. [安全防护](#6-安全防护)
7. [监控告警](#7-监控告警)
8. [开发工具](#8-开发工具)

---

## 1. 框架核心能力

### 能力矩阵

| 分类 | 能力 | 组件 | 说明 |
|------|------|------|------|
| **协议** | 协议分发 | ProtocolDispatcher | 自动路由、参数注入、异常处理 |
| | 协议注册 | @Protocol + @ProtocolController | 注解驱动，零配置 |
| **Actor** | 无锁并发 | ActorSystem | 每实体一个 Actor，单线程处理 |
| | 消息处理 | @MessageHandler | 注解驱动消息处理 |
| **数据** | MongoDB | MongoService + BaseRepository | 文档存储、查询构建器 |
| | Redis | RedisService | 缓存、排行榜、分布式锁 |
| | 二级缓存 | CacheService | L1 Caffeine + L2 Redis |
| **事件** | 本地事件 | EventBus | 解耦模块通信 |
| | 分布式事件 | DistributedEventBus | 跨服务事件 |
| **任务** | 本地定时器 | TimerService | 玩家级别短期任务 |
| | 分布式调度 | XXL-Job | 全局定时任务 |
| **安全** | 请求验证 | RequestValidator | 时间戳 + 签名验证 |
| | 安全过滤 | SecurityFilter | IP黑名单、XSS、敏感词 |
| | 限流 | RateLimiterService | 令牌桶 + 滑动窗口 |
| **监控** | 性能指标 | MetricsService | 计数器、计量器、计时器 |
| | 健康检查 | HealthController | K8s 存活/就绪探针 |
| | 服务监控 | ServerMonitor | CPU、内存、在线人数 |
| **运维** | 优雅停机 | GracefulShutdown | 有序关闭、数据保存 |
| | 链路追踪 | TraceContext | traceId 全链路传递 |
| | 告警通知 | AlertService | 异常、性能、业务告警 |

---

## 2. 协议处理

### 2.1 协议控制器

```java
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_PLAYER, value = "玩家模块")
public class PlayerHandler extends BaseHandler {
    
    @Protocol(methodId = MethodId.Player.GET_INFO, 
              desc = "获取玩家信息", 
              requireLogin = true, 
              requireRole = true)
    public Message getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
        // 框架自动完成: 协议路由、参数解析、登录校验、异常处理
        // 开发者只需关注业务逻辑
        
        PlayerActor actor = playerActorSystem.getActorIfPresent(session.getRoleId());
        if (actor == null) {
            // 抛出异常，框架自动转换为错误响应
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        return S2C_GetPlayerInfo.newBuilder()
                .setResult(buildSuccessResult())
                .setPlayer(buildPlayerInfo(actor.getData()))
                .build();
    }
}
```

### 2.2 协议注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| methodId | int | 0 | 方法号 (与 moduleId 组合) |
| value | int | 0 | 完整协议号 (优先级最高) |
| desc | String | "" | 协议描述 |
| requireLogin | boolean | true | 是否需要登录 |
| requireRole | boolean | true | 是否需要角色 |
| rateLimit | int | 0 | 每秒最大请求数 (0=不限) |
| slowThreshold | int | 100 | 慢请求阈值 (毫秒) |
| async | boolean | false | 是否异步执行 |
| executeInActor | boolean | false | 是否在 Actor 中执行 |

### 2.3 参数自动注入

框架支持以下参数类型的自动注入：

| 参数类型 | 注入内容 |
|---------|---------|
| Session | 当前会话 |
| GameMessage | 原始消息 |
| RequestContext | 请求上下文 |
| Message (Protobuf) | 解析后的请求消息 |
| long roleId | 当前角色 ID |
| long accountId | 当前账号 ID |
| int serverId | 当前服务器 ID |
| int seqId | 请求序号 |

---

## 3. 数据访问

### 3.1 MongoDB 操作

```java
// Repository 继承 BaseRepository
@Repository
public class PlayerRepository extends BaseRepository<PlayerData, Long> {
    
    public PlayerRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, PlayerData.class);
    }
    
    // 使用 QueryBuilder 构建复杂查询
    public List<PlayerData> findTopPlayers(int serverId, int limit) {
        Query query = QueryBuilder.create()
            .eq("serverId", serverId)
            .gt("level", 10)
            .orderByDesc("combatPower")
            .limit(limit)
            .build();
        return find(query);
    }
}
```

### 3.2 Redis 操作

```java
@Service
public class PlayerService {
    @Autowired
    private RedisService redisService;
    
    public void example() {
        // String 操作
        redisService.set("player:123:token", token, Duration.ofHours(24));
        String token = redisService.get("player:123:token");
        
        // Hash 操作
        redisService.hset("player:123", "gold", "1000");
        
        // Sorted Set (排行榜)
        redisService.zadd("rank:combat", roleId, combatPower);
        
        // 分布式锁
        try (var lock = redisService.tryLock("trade:" + roleId, 10)) {
            if (lock.isLocked()) {
                doTrade();
            }
        }
    }
}
```

---

## 4. 缓存系统

### 4.1 二级缓存

```java
// L1: Caffeine (本地内存, ns级)
// L2: Redis (分布式, μs级)

PlayerConfig config = cacheService.get(
    "player_config",           // 缓存名称
    playerId,                   // 缓存 Key
    id -> loadFromDB(id),       // 加载函数
    PlayerConfig.class          // 值类型
);

// 手动失效
cacheService.evict("player_config", playerId);
```

### 4.2 配置加载

```java
@ConfigContainer(file = "item.json", configClass = ItemConfig.class)
@Component
public class ItemConfigContainer extends BaseConfigContainer<ItemConfig> {
    
    public ItemConfig getItem(int itemId) {
        return get(itemId);
    }
    
    public List<ItemConfig> getItemsByType(int type) {
        return getAll().stream()
            .filter(c -> c.getType() == type)
            .toList();
    }
}

// 热更新 (自动广播到集群)
configLoader.reloadAndBroadcast("item.json", "admin");
```

---

## 5. 分布式能力

### 5.1 分布式锁

```java
// 简单使用
lockService.executeWithLock("order:" + orderId, () -> {
    return createOrder();
});

// 带返回值
Order order = lockService.executeWithLock("order:" + orderId, () -> {
    return createOrder();
});

// 带重试
lockService.executeWithLock("trade", 3, 100, () -> {
    doTrade();
});
```

### 5.2 分布式事件

```java
// 发布事件 (自动广播到所有服务实例)
distributedEventBus.publish(new PlayerLevelUpEvent(roleId, newLevel));

// 监听事件
@Component
public class RewardService {
    @EventListener
    public void onLevelUp(PlayerLevelUpEvent event) {
        sendLevelUpReward(event.getRoleId(), event.getNewLevel());
    }
}
```

### 5.3 分布式 ID

```java
// 自动分配 workerId，支持多机部署
long id = idService.nextId();
long playerId = idService.nextPlayerId();
long orderId = idService.nextOrderId();
```

### 5.4 服务注册

```java
// 服务实例自动注册到 Redis，支持集群感知
List<ServiceInstance> instances = serviceRegistry.getInstances("game-service");
```

---

## 6. 安全防护

### 6.1 请求验证

```java
// 自动验证请求时间戳和签名
RequestValidator.ValidationResult result = requestValidator.validate(
    timestamp,
    signature, 
    requestBody
);

if (!result.success()) {
    throw new BizException(ErrorCode.INVALID_REQUEST, result.reason());
}
```

### 6.2 安全过滤

```java
// IP 黑名单检查
if (securityFilter.isIpBlocked(ip)) {
    throw new BizException(ErrorCode.IP_BLOCKED);
}

// XSS 过滤
String safeContent = securityFilter.filterXss(userInput);

// 敏感词过滤
String filteredContent = securityFilter.filterSensitiveWords(chatContent);

// 昵称验证
if (!securityFilter.validateNickname(nickname)) {
    throw new BizException(ErrorCode.INVALID_NICKNAME);
}
```

### 6.3 限流

```java
// 玩家操作限流
if (!rateLimiter.tryPlayerLimit(roleId, "chat", 5, Duration.ofSeconds(1))) {
    throw new BizException(ErrorCode.RATE_LIMIT);
}

// IP 限流
rateLimiter.tryIpLimit(ip, "login", 10, Duration.ofMinutes(1));

// 本地令牌桶
rateLimiter.tryAcquire("api:send_sms", 100);  // 每秒100个
```

---

## 7. 监控告警

### 7.1 性能指标

```java
// 计数器
metrics.increment("login.success");

// 计量器
metrics.gauge("online.players", onlineCount);

// 计时器
try (var t = metrics.timer("db.query")) {
    queryDatabase();
}
```

### 7.2 健康检查

```
GET /health/live   -> 存活探针
GET /health/ready  -> 就绪探针
GET /health        -> 详细健康信息
GET /health/stats  -> 服务器统计
```

### 7.3 告警通知

```java
// 异常告警
alertService.alertException("订单创建失败", exception);

// 性能告警
alertService.alertPerformance("createOrder", costMs, threshold);

// 业务告警
alertService.alertBusiness("充值异常", "用户 " + roleId + " 充值金额异常");
```

---

## 8. 开发工具

### 8.1 工具类

| 工具类 | 功能 |
|--------|------|
| RandomUtil | 权重随机、概率命中 |
| TimeUtil | 时间计算、格式化 |
| MathUtil | 安全计算、百分比、伤害公式 |
| CryptoUtil | MD5、AES、签名 |
| RetryUtil | 重试逻辑 |
| Validator | 参数校验 |

### 8.2 参数校验

```java
// 静态校验
Validator.notNull(player, ErrorCode.ROLE_NOT_FOUND);
Validator.notEmpty(name, ErrorCode.PARAM_ERROR, "名称不能为空");
Validator.positive(gold, ErrorCode.PARAM_ERROR);
Validator.range(level, 1, 100, ErrorCode.PARAM_ERROR);
```

### 8.3 链路追踪

```java
// 自动集成 - 每个请求自动生成 traceId
// 日志自动携带 traceId 和 roleId

// Logback 配置
// %d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] [%X{roleId}] %-5level - %msg%n

// 手动获取
String traceId = TraceContext.getTraceId();
```

### 8.4 优雅停机

```java
@Component
public class PlayerService implements ShutdownAware {
    @Override
    public int getShutdownOrder() {
        return 500;  // 数字越大越先执行
    }

    @Override
    public void onShutdown() {
        saveAllPlayers();
    }
}

// 停机顺序建议:
// 1000+ : 网关层 (停止接收新连接)
// 500-999 : 业务层 (保存玩家数据)
// 100-499 : 框架层 (停止定时任务)
// 0-99 : 基础设施层 (关闭数据库连接)
```

---

## 快速上手

### 1. 创建协议处理器

```java
@ProtocolController(moduleId = 0x10, value = "商城模块")
public class ShopHandler extends BaseHandler {
    
    @Protocol(methodId = 0x01, desc = "购买商品")
    public Message buy(Session session, C2S_Buy request) {
        long roleId = session.getRoleId();
        
        // 业务逻辑...
        
        return S2C_Buy.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }
}
```

### 2. 创建服务类

```java
@Service
public class ShopService extends BaseService {
    
    public void buyItem(long roleId, int itemId, int count) {
        // 使用基类方法
        checkPositive(count, ErrorCode.PARAM_ERROR);
        
        // 业务逻辑...
        
        // 发布事件
        publishEvent(new ItemBoughtEvent(roleId, itemId, count));
        
        // 推送消息
        push(roleId, 0x1001, buildPushMessage());
    }
}
```

### 3. 创建配置容器

```java
@ConfigContainer(file = "shop.json", configClass = ShopConfig.class)
@Component
public class ShopConfigContainer extends BaseConfigContainer<ShopConfig> {
}
```

---

## 设计原则

1. **约定优于配置** - 遵循命名规范即可，无需大量配置
2. **注解驱动** - @Protocol、@EventListener 自动注册
3. **便捷方法** - 常用操作都有简化方法
4. **统一异常** - BizException 自动转换为客户端响应
5. **上下文传递** - RequestContext 无需参数传递
6. **多机部署** - 天然支持分布式，无需额外配置
7. **优雅停机** - 实现 ShutdownAware 接口自动处理
8. **链路追踪** - 自动生成 traceId，日志自动携带

---

**文档版本**: v1.0  
**最后更新**: 2026-01
