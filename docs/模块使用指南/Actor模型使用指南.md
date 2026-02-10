# Actor 模型使用指南

> **模块**: `framework-actor`  
> **功能**: 无锁化并发模型，实体状态管理  
> **版本**: v1.0

---

## 📚 目录

1. [概述](#概述)
2. [核心概念](#核心概念)
3. [创建 Actor](#创建-actor)
4. [创建 ActorSystem](#创建-actorsystem)
5. [使用 Actor](#使用-actor)
6. [最佳实践](#最佳实践)

---

## 概述

Actor 模型是一种并发编程模型，每个 Actor 是独立的执行单元：

| 特性 | 说明 |
|-----|------|
| **单线程处理** | 每个 Actor 内部顺序处理消息，无需加锁 |
| **消息驱动** | 通过消息队列通信，解耦调用方和处理方 |
| **状态封装** | Actor 持有并管理自己的数据状态 |
| **自动持久化** | 定期自动保存脏数据到数据库 |
| **自动淘汰** | 空闲 Actor 自动从内存中移除 |

### 适用场景

- **PlayerActor**: 每个在线玩家对应一个 Actor，管理玩家数据和状态
- **GuildActor**: 每个公会对应一个 Actor，管理公会数据
- **RoomActor**: 每个房间/副本对应一个 Actor

---

## 核心概念

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     ActorSystem                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Actor Cache (Caffeine)                ││
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    ││
│  │  │ Actor 1 │  │ Actor 2 │  │ Actor 3 │  │ Actor N │    ││
│  │  │ ID:1001 │  │ ID:1002 │  │ ID:1003 │  │ ID:NNNN │    ││
│  │  │ mailbox │  │ mailbox │  │ mailbox │  │ mailbox │    ││
│  │  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘    ││
│  └───────│────────────│────────────│────────────│──────────┘│
│          │            │            │            │           │
│  ┌───────▼────────────▼────────────▼────────────▼──────────┐│
│  │              Virtual Thread Executor                     ││
│  └──────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 核心类

| 类 | 说明 |
|---|------|
| `Actor<T>` | Actor 基类，T 是数据类型 |
| `ActorSystem<T>` | Actor 管理器，管理 Actor 生命周期 |
| `ActorMessage` | Actor 间通信的消息 |
| `ActorSystemConfig` | ActorSystem 配置 |

---

## 创建 Actor

### 1. 定义 Actor 数据类

```java
package com.game.service.game.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家数据
 */
@Data
@Document(collection = "player")
public class PlayerData {
    
    @Id
    private Long id;              // 角色ID
    
    private Long accountId;       // 账号ID
    private String name;          // 角色名
    private int level;            // 等级
    private long exp;             // 经验
    private long gold;            // 金币
    private long diamond;         // 钻石
    private int vipLevel;         // VIP等级
    
    // 背包数据
    private Map<Long, BagItem> bagItems = new HashMap<>();
    
    // 任务数据
    private Map<Integer, QuestData> quests = new HashMap<>();
    
    private long createTime;
    private long updateTime;
    private long lastLoginTime;
}
```

### 2. 创建 Actor 类

```java
package com.game.service.game.actor;

import com.game.actor.core.Actor;
import com.game.actor.core.ActorMessage;
import com.game.core.event.EventBus;
import com.game.service.game.entity.PlayerData;
import com.game.entity.repository.PlayerRepository;
import com.game.service.game.event.PlayerEvents;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 Actor
 * <p>
 * 每个在线玩家对应一个 PlayerActor 实例
 * </p>
 */
@Slf4j
public class PlayerActor extends Actor<PlayerData> {

    private final PlayerRepository playerRepository;
    private final EventBus eventBus;

    // ==================== 消息类型常量 ====================
    public static final String MSG_ADD_EXP = "ADD_EXP";
    public static final String MSG_ADD_GOLD = "ADD_GOLD";
    public static final String MSG_ADD_ITEM = "ADD_ITEM";
    public static final String MSG_USE_ITEM = "USE_ITEM";
    public static final String MSG_LEVEL_UP = "LEVEL_UP";

    public PlayerActor(long roleId, PlayerRepository playerRepository, EventBus eventBus) {
        super(roleId, "PLAYER", 1000);  // actorId, actorType, 最大消息队列大小
        this.playerRepository = playerRepository;
        this.eventBus = eventBus;
    }

    // ==================== 生命周期方法 ====================

    /**
     * 加载玩家数据 (Actor 启动时调用)
     */
    @Override
    protected PlayerData loadData() {
        PlayerData data = playerRepository.getById(getActorId());
        if (data != null) {
            data.setLastLoginTime(System.currentTimeMillis());
            log.info("加载玩家数据成功: roleId={}, name={}, level={}", 
                    getActorId(), data.getName(), data.getLevel());
        }
        return data;
    }

    /**
     * 保存玩家数据 (定期自动调用 + Actor 停止时调用)
     */
    @Override
    protected void saveData() {
        if (data != null) {
            data.setUpdateTime(System.currentTimeMillis());
            playerRepository.save(data);
            log.debug("保存玩家数据: roleId={}", getActorId());
        }
    }

    /**
     * Actor 停止前的清理工作
     */
    @Override
    protected void onStop() {
        log.info("玩家下线: roleId={}, name={}", getActorId(), data.getName());
        // 发布下线事件
        eventBus.publish(new PlayerEvents.PlayerOfflineEvent(getActorId()));
    }

    // ==================== 消息处理 ====================

    /**
     * 处理消息
     */
    @Override
    protected void handleMessage(ActorMessage message) {
        switch (message.getType()) {
            case MSG_ADD_EXP -> handleAddExp(message);
            case MSG_ADD_GOLD -> handleAddGold(message);
            case MSG_ADD_ITEM -> handleAddItem(message);
            case MSG_USE_ITEM -> handleUseItem(message);
            default -> log.warn("未知消息类型: {}", message.getType());
        }
    }

    private void handleAddExp(ActorMessage message) {
        long expToAdd = (Long) message.getData();
        int oldLevel = data.getLevel();
        
        data.setExp(data.getExp() + expToAdd);
        markDirty();  // 标记数据需要保存
        
        // 检查升级
        while (canLevelUp()) {
            levelUp();
        }
        
        if (data.getLevel() > oldLevel) {
            // 发布升级事件
            eventBus.publish(new PlayerEvents.PlayerLevelUpEvent(
                    getActorId(), oldLevel, data.getLevel()));
        }
    }

    private void handleAddGold(ActorMessage message) {
        long goldToAdd = (Long) message.getData();
        data.setGold(data.getGold() + goldToAdd);
        markDirty();
    }

    private void handleAddItem(ActorMessage message) {
        // 处理添加物品逻辑
        markDirty();
    }

    private void handleUseItem(ActorMessage message) {
        // 处理使用物品逻辑
        markDirty();
    }

    // ==================== 业务方法 ====================

    private boolean canLevelUp() {
        // 检查是否满足升级条件
        long expRequired = getExpRequired(data.getLevel() + 1);
        return data.getExp() >= expRequired;
    }

    private void levelUp() {
        long expRequired = getExpRequired(data.getLevel() + 1);
        data.setExp(data.getExp() - expRequired);
        data.setLevel(data.getLevel() + 1);
        log.info("玩家升级: roleId={}, newLevel={}", getActorId(), data.getLevel());
    }

    private long getExpRequired(int level) {
        // 从配置表获取升级所需经验
        return level * 100L;
    }

    // ==================== 对外接口 (同步调用) ====================

    /**
     * 获取玩家等级 (直接读取，线程安全)
     */
    public int getLevel() {
        return data != null ? data.getLevel() : 0;
    }

    /**
     * 获取玩家金币
     */
    public long getGold() {
        return data != null ? data.getGold() : 0;
    }

    /**
     * 获取玩家信息快照
     */
    public PlayerSnapshot getSnapshot() {
        if (data == null) return null;
        return new PlayerSnapshot(
                data.getId(),
                data.getName(),
                data.getLevel(),
                data.getGold(),
                data.getDiamond()
        );
    }

    public record PlayerSnapshot(long roleId, String name, int level, long gold, long diamond) {}
}
```

---

## 创建 ActorSystem

### 1. 创建 ActorSystem Bean

```java
package com.game.service.game.actor;

import com.game.actor.core.ActorSystem;
import com.game.actor.core.ActorSystem.ActorSystemConfig;
import com.game.core.event.EventBus;
import com.game.entity.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ActorConfig {

    private final PlayerRepository playerRepository;
    private final EventBus eventBus;

    @Bean
    public ActorSystem<PlayerActor> playerActorSystem() {
        ActorSystemConfig config = ActorSystemConfig.create()
                .maxSize(10000)           // 最大缓存 10000 个 Actor
                .idleTimeoutMinutes(30)   // 空闲 30 分钟后淘汰
                .saveIntervalSeconds(300); // 每 5 分钟自动保存

        return new ActorSystem<>(
                "PlayerActorSystem",
                config,
                roleId -> new PlayerActor(roleId, playerRepository, eventBus)
        );
    }
}
```

### 2. 创建 ActorSystem 服务封装

```java
package com.game.service.game.actor;

import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 玩家 ActorSystem 服务
 * <p>
 * 封装 ActorSystem 操作，提供便捷的业务接口
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerActorSystem {

    private final ActorSystem<PlayerActor> actorSystem;

    // ==================== Actor 管理 ====================

    /**
     * 获取玩家 Actor (自动创建)
     */
    public PlayerActor getActor(long roleId) {
        return actorSystem.getActor(roleId);
    }

    /**
     * 获取玩家 Actor (不自动创建)
     */
    public PlayerActor getActorIfPresent(long roleId) {
        return actorSystem.getActorIfPresent(roleId);
    }

    /**
     * 判断玩家是否在线
     */
    public boolean isOnline(long roleId) {
        return actorSystem.hasActor(roleId);
    }

    /**
     * 下线玩家
     */
    public void offline(long roleId) {
        actorSystem.removeActor(roleId);
    }

    /**
     * 获取在线玩家数量
     */
    public long getOnlineCount() {
        return actorSystem.getActorCount();
    }

    // ==================== 消息发送 ====================

    /**
     * 发送消息到玩家 Actor
     */
    public boolean tell(long roleId, ActorMessage message) {
        return actorSystem.tell(roleId, message);
    }

    /**
     * 发送消息 (仅当玩家在线时)
     */
    public boolean tellIfOnline(long roleId, ActorMessage message) {
        return actorSystem.tellIfPresent(roleId, message);
    }

    // ==================== 便捷方法 ====================

    /**
     * 增加经验
     */
    public void addExp(long roleId, long exp) {
        tell(roleId, ActorMessage.of(PlayerActor.MSG_ADD_EXP, exp));
    }

    /**
     * 增加金币
     */
    public void addGold(long roleId, long gold) {
        tell(roleId, ActorMessage.of(PlayerActor.MSG_ADD_GOLD, gold));
    }

    /**
     * 增加物品
     */
    public void addItem(long roleId, int itemId, int count) {
        tell(roleId, ActorMessage.of(PlayerActor.MSG_ADD_ITEM, new int[]{itemId, count}));
    }

    /**
     * 获取玩家快照信息
     */
    public PlayerActor.PlayerSnapshot getPlayerSnapshot(long roleId) {
        PlayerActor actor = getActorIfPresent(roleId);
        return actor != null ? actor.getSnapshot() : null;
    }
}
```

---

## 使用 Actor

### 1. 在 Handler 中使用

```java
@Slf4j
@ProtocolController(module = "player", desc = "玩家模块")
@RequiredArgsConstructor
public class PlayerHandler {

    private final PlayerActorSystem playerActorSystem;
    private final DubboPushService pushService;

    /**
     * 获取玩家信息
     */
    @Protocol(id = MethodId.PLAYER_GET_INFO)
    public void getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
        long roleId = session.getRoleId();
        
        // 获取 Actor
        PlayerActor actor = playerActorSystem.getActor(roleId);
        if (actor == null) {
            throw new BizException(ErrorCode.PLAYER_NOT_FOUND);
        }
        
        // 获取数据快照
        PlayerActor.PlayerSnapshot snapshot = actor.getSnapshot();
        
        // 返回响应
        S2C_PlayerInfo response = S2C_PlayerInfo.newBuilder()
                .setRoleId(snapshot.roleId())
                .setName(snapshot.name())
                .setLevel(snapshot.level())
                .setGold(snapshot.gold())
                .setDiamond(snapshot.diamond())
                .build();
                
        session.send(MethodId.PLAYER_GET_INFO, response);
    }

    /**
     * 使用物品
     */
    @Protocol(id = MethodId.PLAYER_USE_ITEM)
    public void useItem(Session session, C2S_UseItem request) {
        long roleId = session.getRoleId();
        
        // 发送消息到 Actor (异步处理)
        playerActorSystem.tell(roleId, ActorMessage.of(
                PlayerActor.MSG_USE_ITEM,
                new UseItemData(request.getItemId(), request.getCount())
        ));
        
        // 如果需要同步返回结果，可以直接调用 Actor 方法
        // PlayerActor actor = playerActorSystem.getActor(roleId);
        // boolean success = actor.useItem(request.getItemId(), request.getCount());
    }
}
```

### 2. 在定时任务中使用

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerDailyTask {

    private final PlayerActorSystem playerActorSystem;

    /**
     * 每日零点重置所有在线玩家
     */
    @XxlJob("playerDailyReset")
    public void dailyReset() {
        log.info("开始执行玩家每日重置");
        
        // 遍历所有在线玩家 Actor
        for (PlayerActor actor : playerActorSystem.getActorSystem().getAllActors()) {
            actor.tell(ActorMessage.of("DAILY_RESET", null));
        }
        
        log.info("玩家每日重置完成，在线人数: {}", playerActorSystem.getOnlineCount());
    }
}
```

### 3. 在事件监听中使用

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final PlayerActorSystem playerActorSystem;
    private final DubboPushService pushService;

    /**
     * 监听玩家升级事件
     */
    @EventListener
    public void onPlayerLevelUp(PlayerEvents.PlayerLevelUpEvent event) {
        log.info("玩家升级: roleId={}, {} -> {}", 
                event.getRoleId(), event.getOldLevel(), event.getNewLevel());
        
        // 发放升级奖励
        playerActorSystem.addGold(event.getRoleId(), event.getNewLevel() * 1000);
        
        // 推送升级通知
        pushService.pushToPlayer(event.getRoleId(), MethodId.PUSH_LEVEL_UP,
                S2C_LevelUp.newBuilder()
                        .setLevel(event.getNewLevel())
                        .build());
    }
}
```

---

## 最佳实践

### 1. 消息类型定义

```java
/**
 * 玩家 Actor 消息类型
 */
public final class PlayerActorMessages {
    
    // 资源相关
    public static final String ADD_EXP = "ADD_EXP";
    public static final String ADD_GOLD = "ADD_GOLD";
    public static final String ADD_DIAMOND = "ADD_DIAMOND";
    
    // 背包相关
    public static final String ADD_ITEM = "ADD_ITEM";
    public static final String REMOVE_ITEM = "REMOVE_ITEM";
    public static final String USE_ITEM = "USE_ITEM";
    
    // 任务相关
    public static final String ACCEPT_QUEST = "ACCEPT_QUEST";
    public static final String COMPLETE_QUEST = "COMPLETE_QUEST";
    
    // 系统相关
    public static final String DAILY_RESET = "DAILY_RESET";
    public static final String SAVE_DATA = "SAVE_DATA";
    
    private PlayerActorMessages() {}
}
```

### 2. 复杂消息数据

```java
// 使用 record 定义消息数据
public record AddItemData(int itemId, int count, String source) {}
public record UseItemData(long itemUid, int count) {}
public record TransferGoldData(long targetRoleId, long amount) {}

// 发送复杂消息
playerActorSystem.tell(roleId, ActorMessage.of(
        PlayerActorMessages.ADD_ITEM,
        new AddItemData(1001, 10, "quest_reward")
));
```

### 3. 处理跨 Actor 通信

```java
@Slf4j
public class PlayerActor extends Actor<PlayerData> {

    private final GuildActorSystem guildActorSystem;

    /**
     * 处理加入公会
     */
    private void handleJoinGuild(ActorMessage message) {
        long guildId = (Long) message.getData();
        
        // 发送消息到公会 Actor
        boolean success = guildActorSystem.tell(guildId, ActorMessage.of(
                GuildActor.MSG_ADD_MEMBER,
                new MemberJoinData(getActorId(), data.getName(), data.getLevel())
        ));
        
        if (success) {
            data.setGuildId(guildId);
            markDirty();
        }
    }
}
```

### 4. 同步 vs 异步处理

```java
// 异步处理 (推荐) - 通过消息队列
public void addExpAsync(long roleId, long exp) {
    actorSystem.tell(roleId, ActorMessage.of(MSG_ADD_EXP, exp));
}

// 同步处理 - 直接调用 Actor 方法
public int getLevel(long roleId) {
    PlayerActor actor = actorSystem.getActorIfPresent(roleId);
    return actor != null ? actor.getLevel() : 0;
}

// 同步处理复杂逻辑 (需要返回结果)
public boolean useItem(long roleId, long itemUid, int count) {
    PlayerActor actor = actorSystem.getActor(roleId);
    if (actor == null) {
        return false;
    }
    // 直接调用 Actor 的同步方法
    return actor.useItemSync(itemUid, count);
}
```

### 5. Actor 配置调优

```java
// 根据业务场景调整配置
ActorSystemConfig config = ActorSystemConfig.create()
        // 最大缓存数量 (根据服务器内存调整)
        .maxSize(20000)
        
        // 空闲超时 (分钟) - 长时间不活跃的 Actor 会被淘汰
        .idleTimeoutMinutes(60)
        
        // 自动保存间隔 (秒) - 定期保存脏数据
        .saveIntervalSeconds(180);
```

---

## 常见问题

### Q1: Actor 和直接操作数据库的区别？

| 方式 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| Actor | 无锁并发、自动缓存、状态管理 | 内存占用 | 在线玩家、活跃公会 |
| 直接数据库 | 简单直接 | 需要加锁、无缓存 | 离线数据查询 |

### Q2: 如何确保 Actor 数据不丢失？

1. **定期自动保存**: `saveIntervalSeconds` 配置自动保存周期
2. **关机时保存**: `ActorSystem.destroy()` 会保存所有 Actor 数据
3. **脏数据标记**: 只有调用 `markDirty()` 的数据才会保存

### Q3: Actor 消息队列满了怎么办？

```java
// 消息队列默认大小 1000
// 满了之后 tell() 返回 false

boolean success = actorSystem.tell(roleId, message);
if (!success) {
    log.warn("玩家消息队列已满: roleId={}", roleId);
    // 可以选择丢弃、重试或返回错误给客户端
}
```

### Q4: 如何监控 Actor 状态？

```java
// 获取在线数量
long onlineCount = actorSystem.getActorCount();

// 遍历所有 Actor 获取详细信息
for (PlayerActor actor : actorSystem.getAllActors()) {
    log.info("Actor: id={}, mailbox={}, dirty={}, lastActive={}",
            actor.getActorId(),
            actor.getMailboxSize(),
            actor.getDirty().get(),
            actor.getLastActiveTime().get());
}
```
