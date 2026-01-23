# Dubbo RPC 使用指南

> **框架**: Apache Dubbo  
> **功能**: 微服务间 RPC 调用  
> **版本**: v1.0

---

## 📚 目录

1. [概述](#概述)
2. [服务提供者](#服务提供者)
3. [服务消费者](#服务消费者)
4. [负载均衡策略](#负载均衡策略)
5. [高级配置](#高级配置)
6. [最佳实践](#最佳实践)

---

## 概述

Dubbo 是高性能 RPC 框架，用于微服务间通信：

| 特性 | 说明 |
|-----|------|
| **服务注册发现** | 基于 Nacos 自动注册和发现服务 |
| **负载均衡** | 支持多种负载均衡策略 |
| **容错机制** | 支持重试、熔断、降级 |
| **链路追踪** | 自动传递 traceId |

### 服务调用流程

```
┌─────────────────┐                           ┌─────────────────┐
│   Game Service  │                           │  Guild Service  │
│                 │                           │                 │
│ @DubboReference │  ──── Dubbo RPC ────────> │ @DubboService   │
│ guildService    │                           │ GuildServiceImpl│
└─────────────────┘                           └─────────────────┘
         │                                            │
         │                                            │
         └──────────────┬─────────────────────────────┘
                        │
                        ▼
              ┌─────────────────────┐
              │       Nacos         │
              │  (服务注册中心)      │
              └─────────────────────┘
```

---

## 服务提供者

### 1. 定义服务接口

在 `common-api` 模块定义接口：

```java
package com.game.api.guild;

import com.game.common.result.Result;

/**
 * 公会服务接口
 */
public interface GuildService {

    /**
     * 获取玩家所在公会
     */
    Result<GuildDTO> getPlayerGuild(long roleId);

    /**
     * 创建公会
     */
    Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId);

    /**
     * 加入公会
     */
    Result<Void> joinGuild(long roleId, long guildId);

    /**
     * 退出公会
     */
    Result<Void> leaveGuild(long roleId);

    /**
     * 解散公会
     */
    Result<Void> dissolveGuild(long roleId, long guildId);

    /**
     * 每日重置
     */
    Result<Void> dailyReset();
}
```

### 2. 实现服务

在服务模块实现接口：

```java
package com.game.service.guild.impl;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.service.guild.service.GuildBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 公会服务实现
 */
@Slf4j
@DubboService(
    version = "1.0.0",
    group = "GAME_SERVER",
    timeout = 5000,
    retries = 0
)
@RequiredArgsConstructor
public class GuildServiceImpl implements GuildService {

    private final GuildBusinessService businessService;

    @Override
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        try {
            GuildDTO guild = businessService.getPlayerGuild(roleId);
            return Result.success(guild);
        } catch (Exception e) {
            log.error("获取玩家公会失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId) {
        try {
            GuildDTO guild = businessService.createGuild(roleId, guildName, declaration, iconId);
            return Result.success(guild);
        } catch (BizException e) {
            return Result.fail(e.getErrorCode());
        } catch (Exception e) {
            log.error("创建公会失败: roleId={}, name={}", roleId, guildName, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> joinGuild(long roleId, long guildId) {
        try {
            businessService.joinGuild(roleId, guildId);
            return Result.success();
        } catch (BizException e) {
            return Result.fail(e.getErrorCode());
        } catch (Exception e) {
            log.error("加入公会失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> leaveGuild(long roleId) {
        try {
            businessService.leaveGuild(roleId);
            return Result.success();
        } catch (BizException e) {
            return Result.fail(e.getErrorCode());
        } catch (Exception e) {
            log.error("退出公会失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> dissolveGuild(long roleId, long guildId) {
        try {
            businessService.dissolveGuild(roleId, guildId);
            return Result.success();
        } catch (BizException e) {
            return Result.fail(e.getErrorCode());
        } catch (Exception e) {
            log.error("解散公会失败: roleId={}, guildId={}", roleId, guildId, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Result<Void> dailyReset() {
        try {
            businessService.dailyReset();
            return Result.success();
        } catch (Exception e) {
            log.error("公会每日重置失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR);
        }
    }
}
```

### 3. @DubboService 参数说明

```java
@DubboService(
    // 版本号 (用于灰度发布、多版本并存)
    version = "1.0.0",
    
    // 服务分组 (区分不同环境/集群)
    group = "GAME_SERVER",
    
    // 超时时间 (毫秒)
    timeout = 5000,
    
    // 重试次数 (不含首次调用)
    retries = 0,
    
    // 负载均衡策略
    loadbalance = "random",
    
    // 集群容错策略
    cluster = "failfast",
    
    // 权重 (用于加权负载均衡)
    weight = 100,
    
    // 服务预热时间 (毫秒)
    warmup = 10000,
    
    // 并发限制 (-1 表示不限制)
    executes = -1,
    
    // 接口级别配置
    methods = {
        @Method(name = "createGuild", timeout = 10000),
        @Method(name = "dailyReset", retries = 2)
    }
)
```

---

## 服务消费者

### 1. 基本调用

```java
package com.game.service.game.rpc;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RpcServiceCaller {

    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 5000,
        retries = 0,
        check = false  // 启动时不检查服务是否可用
    )
    private GuildService guildService;

    /**
     * 获取玩家公会
     */
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        try {
            return guildService.getPlayerGuild(roleId);
        } catch (Exception e) {
            log.error("调用公会服务失败: roleId={}", roleId, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    /**
     * 创建公会
     */
    public Result<GuildDTO> createGuild(long roleId, String name, String declaration, int iconId) {
        try {
            return guildService.createGuild(roleId, name, declaration, iconId);
        } catch (Exception e) {
            log.error("调用公会服务失败: roleId={}, name={}", roleId, name, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }
}
```

### 2. @DubboReference 参数说明

```java
@DubboReference(
    // 版本号 (必须与提供者一致)
    version = "1.0.0",
    
    // 服务分组
    group = "GAME_SERVER",
    
    // 超时时间 (毫秒)
    timeout = 5000,
    
    // 重试次数
    retries = 0,
    
    // 启动时检查服务是否可用
    check = false,
    
    // 负载均衡策略
    loadbalance = "roundrobin",
    
    // 集群容错策略
    cluster = "failfast",
    
    // 每个方法最大并发调用数
    actives = 0,  // 0 表示不限制
    
    // 直连地址 (开发调试用)
    url = "",
    
    // 服务降级
    mock = "fail:return null",
    
    // 一致性哈希配置
    parameters = {
        "hash.arguments", "0",    // 按第一个参数哈希
        "hash.nodes", "160"       // 虚拟节点数
    }
)
```

---

## 负载均衡策略

### 1. 随机 (random) - 默认

```java
@DubboReference(loadbalance = "random")
private GuildService guildService;

// 随机选择一个服务实例
// 可通过 weight 设置权重
```

### 2. 轮询 (roundrobin)

```java
@DubboReference(loadbalance = "roundrobin")
private RankService rankService;

// 依次选择服务实例
// 适用于无状态服务
```

### 3. 一致性哈希 (consistenthash)

```java
@DubboReference(
    loadbalance = "consistenthash",
    parameters = {
        "hash.arguments", "0",    // 按第一个参数做哈希
        "hash.nodes", "160"       // 虚拟节点数
    }
)
private PlayerService playerService;

// 相同参数的请求总是路由到同一服务实例
// 适用于有状态服务 (如 Actor 模型)
```

### 4. 最少活跃 (leastactive)

```java
@DubboReference(loadbalance = "leastactive")
private ChatService chatService;

// 选择当前活跃调用数最少的服务实例
// 适用于处理耗时不均的服务
```

### 5. 最短响应时间 (shortestresponse)

```java
@DubboReference(loadbalance = "shortestresponse")
private RankService rankService;

// 选择响应时间最短的服务实例
// 根据历史响应时间动态调整
```

---

## 高级配置

### 1. 集群容错策略

```java
// 快速失败 (failfast) - 默认
// 只调用一次，失败立即报错
@DubboReference(cluster = "failfast")
private GuildService guildService;

// 失败重试 (failover)
// 失败自动重试其他服务器
@DubboReference(cluster = "failover", retries = 2)
private RankService rankService;

// 失败安全 (failsafe)
// 出现异常时忽略，返回空结果
@DubboReference(cluster = "failsafe")
private LogService logService;

// 失败自动恢复 (failback)
// 失败后定时重试
@DubboReference(cluster = "failback")
private NotifyService notifyService;

// 并行调用 (forking)
// 并行调用多个服务器，只要一个成功即返回
@DubboReference(cluster = "forking", forks = 2)
private PlayerService playerService;

// 广播调用 (broadcast)
// 广播调用所有服务实例
@DubboReference(cluster = "broadcast")
private BroadcastService broadcastService;
```

### 2. 服务降级

```java
// Mock 降级
@DubboReference(
    mock = "fail:return null"  // 调用失败返回 null
)
private GuildService guildService;

// 自定义 Mock 类
@DubboReference(
    mock = "com.game.service.game.mock.GuildServiceMock"
)
private GuildService guildService;

// Mock 类实现
public class GuildServiceMock implements GuildService {
    @Override
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE);
    }
}
```

### 3. 直连调试

```java
// 开发环境直接连接服务，跳过注册中心
@DubboReference(
    url = "dubbo://192.168.1.100:20880"
)
private GuildService guildService;
```

### 4. 异步调用

```java
@Service
public class AsyncRpcCaller {

    @DubboReference(async = true)
    private GuildService guildService;

    public void asyncCall(long roleId) {
        // 异步调用
        guildService.getPlayerGuild(roleId);
        
        // 获取 Future
        CompletableFuture<Result<GuildDTO>> future = RpcContext.getContext().getCompletableFuture();
        
        // 异步处理结果
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("异步调用失败", exception);
            } else {
                log.info("公会信息: {}", result.getData());
            }
        });
    }
}
```

### 5. 泛化调用 (无需依赖接口)

```java
@Service
public class GenericRpcCaller {

    @DubboReference(interfaceClass = GenericService.class)
    private GenericService genericService;

    public Object genericCall(String methodName, Object... args) {
        // 泛化调用
        return genericService.$invoke(
            methodName,
            new String[]{"long"},  // 参数类型
            args                    // 参数值
        );
    }
}
```

---

## 最佳实践

### 1. 统一的 RPC 调用器

```java
package com.game.service.game.rpc;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.api.rank.RankEntryDTO;
import com.game.api.rank.RankService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RPC 服务调用器
 * <p>
 * 统一管理所有 RPC 调用，提供错误处理和日志记录
 * </p>
 */
@Slf4j
@Service
public class RpcServiceCaller {

    // ==================== 玩家服务 ====================
    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 3000,
        retries = 0,
        loadbalance = "consistenthash",
        parameters = {"hash.arguments", "0"},
        check = false
    )
    private PlayerService playerService;

    // ==================== 公会服务 ====================
    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 5000,
        retries = 0,
        loadbalance = "consistenthash",
        parameters = {"hash.arguments", "0"},
        check = false
    )
    private GuildService guildService;

    // ==================== 排行服务 ====================
    @DubboReference(
        version = "1.0.0",
        group = "GAME_SERVER",
        timeout = 3000,
        retries = 1,
        loadbalance = "roundrobin",
        check = false
    )
    private RankService rankService;

    // ==================== 玩家相关 ====================

    public Result<PlayerDTO> getPlayerInfo(long roleId) {
        return callWithErrorHandling(() -> playerService.getPlayerInfo(roleId), 
                "getPlayerInfo", roleId);
    }

    // ==================== 公会相关 ====================

    public Result<GuildDTO> getPlayerGuild(long roleId) {
        return callWithErrorHandling(() -> guildService.getPlayerGuild(roleId), 
                "getPlayerGuild", roleId);
    }

    public Result<GuildDTO> createGuild(long roleId, String name, String declaration, int iconId) {
        return callWithErrorHandling(() -> guildService.createGuild(roleId, name, declaration, iconId), 
                "createGuild", roleId, name);
    }

    // ==================== 排行相关 ====================

    public Result<List<RankEntryDTO>> getTopRank(String rankType, int limit) {
        return callWithErrorHandling(() -> rankService.getTopN(rankType, limit), 
                "getTopRank", rankType, limit);
    }

    // ==================== 通用错误处理 ====================

    private <T> Result<T> callWithErrorHandling(RpcCall<T> call, String method, Object... params) {
        try {
            return call.execute();
        } catch (Exception e) {
            log.error("RPC调用失败 - {}: params={}", method, params, e);
            return Result.fail(ErrorCode.RPC_ERROR);
        }
    }

    @FunctionalInterface
    interface RpcCall<T> {
        Result<T> execute();
    }
}
```

### 2. 结果处理

```java
public void handleRpcResult() {
    // 调用 RPC
    Result<GuildDTO> result = rpcCaller.getPlayerGuild(roleId);
    
    // 检查结果
    if (!result.isSuccess()) {
        // 处理错误
        log.warn("获取公会失败: code={}, msg={}", result.getCode(), result.getMessage());
        throw new BizException(ErrorCode.of(result.getCode()));
    }
    
    // 获取数据
    GuildDTO guild = result.getData();
    if (guild == null) {
        throw new BizException(ErrorCode.GUILD_NOT_FOUND);
    }
    
    // 使用数据
    processGuild(guild);
}
```

### 3. 超时配置原则

| 场景 | 超时时间 | 重试次数 |
|-----|---------|---------|
| 读取操作 | 3000ms | 1 |
| 写入操作 | 5000ms | 0 |
| 复杂查询 | 10000ms | 0 |
| 定时任务调用 | 30000ms | 0 |

### 4. 启动检查

```java
// 开发环境：关闭启动检查
@DubboReference(check = false)
private GuildService guildService;

// 生产环境：开启启动检查 (确保依赖服务可用)
// application.yml
dubbo:
  consumer:
    check: true
```

---

## 常见问题

### Q1: 如何选择负载均衡策略？

| 服务类型 | 推荐策略 | 原因 |
|---------|---------|------|
| 无状态服务 | roundrobin | 均匀分配负载 |
| 有状态服务 (Actor) | consistenthash | 保证同一实体路由到同一实例 |
| 响应时间敏感 | shortestresponse | 自动选择最快实例 |
| 批处理服务 | random | 简单高效 |

### Q2: RPC 调用失败怎么办？

1. **检查网络**: 确保服务间网络连通
2. **检查注册中心**: 确保服务已注册到 Nacos
3. **检查版本/分组**: 确保 version 和 group 一致
4. **增加超时时间**: 复杂操作适当增加超时
5. **查看日志**: 检查服务端异常日志

### Q3: 如何处理循环依赖？

```java
// 使用懒加载
@DubboReference(lazy = true)
private PlayerService playerService;

// 或使用 @PostConstruct 延迟获取
@PostConstruct
public void init() {
    // 延迟初始化
}
```
