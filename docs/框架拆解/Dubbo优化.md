# Dubbo 能力优化指南

> **版本**: v1.0  
> **更新日期**: 2026-01  
> **设计理念**: 充分利用 Dubbo 能力，减少 Redis 压力

---

## 目录

1. [优化总览](#1-优化总览)
2. [Dubbo 集群模式](#2-dubbo-集群模式)
3. [客户端推送优化](#3-客户端推送优化)
4. [分布式事件优化](#4-分布式事件优化)
5. [服务发现优化](#5-服务发现优化)
6. [Redis 使用场景](#6-redis-使用场景)

---

## 1. 优化总览

### 1.1 优化前后对比

| 功能 | 优化前 | 优化后 | Redis 压力 |
|------|--------|--------|-----------|
| **客户端推送** | Redis Pub/Sub | Dubbo RPC | ✅ 完全消除 |
| **分布式事件** | Redis Pub/Sub | Dubbo Broadcast | ✅ 完全消除 |
| **服务发现** | Redis 存储 | Nacos 直接使用 | ✅ 完全消除 |
| **配置同步** | Redis Pub/Sub | Dubbo Broadcast | ✅ 完全消除 |
| **排行榜** | Redis ZSet | Redis ZSet | 保持不变 |
| **分布式锁** | Redis SETNX | Redis SETNX | 保持不变 |

### 1.2 新增组件

| 组件 | 用途 |
|------|------|
| `DubboPushService` | 基于 Dubbo 的客户端消息推送 |
| `PushTargetService` | Gateway 层推送接口 |
| `DubboEventBus` | 基于 Dubbo 的分布式事件总线 |
| `NacosServiceRegistry` | 基于 Nacos 的服务发现 |

---

## 2. Dubbo 集群模式

### 2.1 可用模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `failover` | 失败自动重试（默认） | 普通 RPC 调用 |
| `failfast` | 快速失败，不重试 | 非幂等操作 |
| `failsafe` | 失败安全，忽略异常 | 日志记录等 |
| `broadcast` | 广播到所有提供者 | 配置刷新、全服推送 |
| `forking` | 并行调用，取最快返回 | 实时性要求高的查询 |

### 2.2 负载均衡策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `random` | 随机（默认） | 无状态服务 |
| `roundrobin` | 轮询 | 无状态服务 |
| `leastactive` | 最少活跃调用 | 慢请求场景 |
| `consistenthash` | 一致性哈希 | 有状态服务（按 roleId/guildId） |

### 2.3 配置示例

```java
// 普通调用 - 一致性哈希
@DubboReference(
    loadbalance = "consistenthash",
    parameters = {"hash.arguments", "0"}  // 按第一个参数哈希
)
private PlayerService playerService;

// 广播调用
@DubboReference(cluster = "broadcast", timeout = 5000)
private BroadcastService broadcastService;
```

---

## 3. 客户端推送优化

### 3.1 架构

```
┌─────────────────────────────────────────────────────────────┐
│                     业务服务层                               │
│   GameService    GuildService    ChatService   RankService  │
│        └──────────────┴───────────────┴─────────────┘       │
│                         │                                   │
│                  DubboPushService                           │
└─────────────────────────┼───────────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              │     Dubbo RPC         │
              │  (broadcast 模式)      │
              └───────────┬───────────┘
                          │
         ┌────────────────┼────────────────┐
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │Gateway 1│      │Gateway 2│      │Gateway 3│
    └────┬────┘      └────┬────┘      └────┬────┘
         │ 本地推送        │ 本地推送       │ 本地推送
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │ Clients │      │ Clients │      │ Clients │
    └─────────┘      └─────────┘      └─────────┘
```

### 3.2 推送类型

| 方法 | Dubbo 模式 | 说明 |
|------|------------|------|
| `pushToPlayer()` | 一致性哈希 | 按 roleId 路由 |
| `pushToPlayers()` | broadcast | 各 Gateway 筛选 |
| `pushToGuild()` | broadcast | 按 guildId 筛选 |
| `broadcast()` | broadcast | 全服推送 |
| `pushToScene()` | broadcast | 场景内推送 |
| `pushToNearby()` | broadcast | AOI 推送 |

### 3.3 使用示例

```java
@Autowired
private DubboPushService pushService;

// 单播
pushService.pushToPlayer(roleId, protocolId, message);

// 公会广播
pushService.pushToGuild(guildId, protocolId, message);

// 全服广播
pushService.broadcast(protocolId, systemNotice);

// 场景同步
pushService.pushToScene(sceneId, protocolId, battleSync);
```

---

## 4. 分布式事件优化

### 4.1 对比

| 特性 | Redis Pub/Sub | Dubbo Broadcast |
|------|---------------|-----------------|
| 可靠性 | 消息可能丢失 | 有返回值 |
| 类型安全 | JSON 字符串 | 接口定义 |
| 超时重试 | 需自行实现 | 内置支持 |
| Redis 压力 | 高 | 无 |

### 4.2 使用示例

```java
@Autowired
private DubboEventBus eventBus;

// 配置刷新
eventBus.broadcastConfigReload("item.json");

// 缓存失效
eventBus.broadcastCacheEvict("player", String.valueOf(roleId));

// 活动状态变更
eventBus.broadcastActivityChange(activityId, 1, activityData);
```

---

## 5. 服务发现优化

### 5.1 使用 Nacos 替代 Redis

```java
@Autowired
private NacosServiceRegistry serviceRegistry;

// 获取服务实例
List<ServiceInstance> instances = serviceRegistry.getInstances("service-game");

// 检查服务可用性
boolean available = serviceRegistry.isServiceAvailable("service-guild");
```

### 5.2 优势

- 无需额外 Redis 存储
- 利用 Nacos 的健康检查
- 与 Dubbo 服务发现共享基础设施

---

## 6. Redis 使用场景

### 6.1 仍需使用 Redis 的场景

| 场景 | 原因 |
|------|------|
| **排行榜** | ZSet 天然支持排序 |
| **分布式锁** | SETNX 原子操作 |
| **会话 Token** | 跨 Gateway 共享 |
| **L2 缓存** | 高性能 KV 存储 |
| **限流计数** | 原子计数 |

### 6.2 优化后的 Redis 压力

```
优化前: 推送 40% + 事件 30% + 服务注册 15% + 其他 15%
优化后: 排行榜 50% + 缓存 20% + 锁/Token 15% + 其他 15%

压力降低约 70%！
```

---

## 最佳实践总结

| 场景 | 推荐方式 |
|------|----------|
| 服务间 RPC 调用 | Dubbo + 一致性哈希 |
| 服务间广播 | Dubbo + broadcast 模式 |
| 客户端推送 | Dubbo → Gateway → WebSocket |
| 排行榜/计数 | Redis ZSet/String |
| 分布式锁 | Redis SETNX |
| 数据缓存 | Caffeine L1 + Redis L2 |
| 服务发现 | Nacos |

---

**文档版本**: v1.0  
**最后更新**: 2026-01
