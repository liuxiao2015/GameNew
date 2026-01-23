# 服务模块实现指南

> **版本**: v1.0  
> **更新日期**: 2026-01  
> **目标**: 为每个服务模块提供完整功能示例，展示框架各项能力的综合运用

---

## 服务模块概览

| 服务 | 端口 | 核心职责 | Actor 模型 |
|------|------|---------|-----------|
| service-login | 8001 | 账号登录、选服、创角、进入游戏 | 无 |
| service-game | 8002 | 玩家核心数据、背包、装备、任务 | PlayerActor |
| service-guild | 8003 | 公会系统、成员管理 | GuildActor |
| service-chat | 8004 | 聊天系统、广播通知 | 无 |
| service-rank | 8005 | 排行榜系统 | 无 |
| service-scheduler | 8006 | 定时任务调度 | 无 |

---

## 服务间调用关系图

```
                        ┌─────────────┐
                        │   Gateway   │
                        └──────┬──────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Login     │◄────►│    Game     │◄────►│   Guild     │
│   Service   │      │   Service   │      │   Service   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                    │
       │              ┌─────┴─────┐              │
       │              ▼           ▼              │
       │     ┌─────────────┐ ┌─────────────┐     │
       │     │    Rank     │ │    Chat     │◄────┘
       │     │   Service   │ │   Service   │
       │     └─────────────┘ └─────────────┘
       │                           │
       └───────────────────────────┘
                     │
              ┌──────▼──────┐
              │  Scheduler  │
              │   Service   │
              └─────────────┘
```

---

## 1. service-login (登录服务)

### 1.1 核心功能

| 功能 | 协议 | 说明 |
|------|------|------|
| 账号登录 | AccountLogin | 游客/账号密码/第三方登录 |
| 绑定账号 | BindAccount | 游客升级为正式账号 |
| 服务器列表 | GetServerList | 获取可进入的服务器 |
| 选择服务器 | SelectServer | 选服并获取角色列表 |
| 创建角色 | CreateRole | 创建新角色 |
| 进入游戏 | EnterGame | 进入游戏，加载玩家数据 |
| 重连 | Reconnect | 断线重连 |

### 1.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| 协议处理 | @ProtocolController | 处理登录相关协议 |
| 参数校验 | Validator | 校验账号、密码、角色名 |
| 安全过滤 | SecurityFilter | 校验角色名合法性、敏感词 |
| 分布式锁 | LockService | 防止重复创建账号/角色 |
| 缓存 | CacheService | Token 缓存、服务器列表缓存 |
| RPC调用 | RpcTemplate | 调用 Game 服务获取玩家数据 |
| 加密 | CryptoUtil | 密码加密、Token 生成 |
| ID生成 | IdService | 账号ID、角色ID生成 |
| 限流 | RateLimiterService | 登录接口限流 |
| 链路追踪 | TraceContext | 登录流程追踪 |

### 1.3 服务间调用

```java
// 进入游戏时调用 Game 服务加载玩家数据
@Protocol(methodId = MethodId.Login.ENTER_GAME, desc = "进入游戏")
public Message enterGame(Session session, C2S_EnterGame request) {
    // 1. 调用 Game 服务获取玩家完整数据
    PlayerFullInfo playerInfo = RpcTemplate.call(
        () -> gameService.loadPlayer(request.getRoleId()),
        ErrorCode.ROLE_NOT_FOUND
    );
    
    // 2. 通知 Chat 服务玩家上线 (广播)
    distributedEventBus.publish(new PlayerOnlineEvent(request.getRoleId()));
    
    // 3. 推送给客户端
    return S2C_EnterGame.newBuilder()
            .setResult(buildSuccessResult())
            .setPlayer(playerInfo)
            .build();
}
```

### 1.4 示例代码结构

```
service-login/src/main/java/com/game/service/login/
├── LoginServiceApplication.java     # 启动类
├── handler/
│   └── LoginHandler.java            # 协议处理器 (完整登录流程)
├── service/
│   ├── AccountServiceImpl.java      # 账号服务 (登录验证)
│   └── LoginServiceImpl.java        # 登录服务 (角色管理)
├── repository/
│   ├── AccountRepository.java       # 账号数据访问
│   └── GameServerRepository.java    # 服务器数据访问
└── entity/
    ├── Account.java                 # 账号实体
    └── GameServer.java              # 服务器实体
```

---

## 2. service-game (游戏服务) ⭐ 核心

### 2.1 核心功能

| 功能 | 协议 | 说明 |
|------|------|------|
| 玩家信息 | GetPlayerInfo | 获取玩家详细信息 |
| 修改信息 | UpdatePlayer | 修改头像、签名等 |
| 改名 | ChangeName | 修改角色名 |
| 背包操作 | GetBag/UseItem/SellItem | 背包物品管理 |
| 装备操作 | Equip/Unequip/Enhance | 装备穿戴和强化 |
| 任务系统 | GetQuest/SubmitQuest | 任务进度和提交 |
| 升级 | - | 经验增加触发升级 |

### 2.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| **Actor模型** | PlayerActor + ActorSystem | 玩家数据无锁化处理 |
| 协议处理 | @ProtocolController | 处理玩家相关协议 |
| 消息处理 | @MessageHandler | Actor 内部消息处理 |
| 事件发布 | EventBus | 发布升级、获得物品等事件 |
| 分布式事件 | DistributedEventBus | 跨服务事件广播 |
| 推送消息 | PushService | 推送属性变化、物品获得 |
| 定时器 | TimerService | Buff 过期、体力恢复 |
| 配置加载 | ConfigContainer | 物品、装备、任务配置 |
| 数据同步 | DataSyncService | 增量数据同步到客户端 |
| 排行榜 | RankService | 更新战力/等级排行 |
| 缓存 | CacheService | 玩家数据缓存 |
| 随机 | RandomUtil | 掉落、强化概率 |
| 数学计算 | MathUtil | 属性计算、伤害公式 |
| 参数校验 | Validator | 校验操作参数 |
| 链路追踪 | TraceContext | 操作追踪 |
| 优雅停机 | ShutdownAware | 保存所有在线玩家 |

### 2.3 服务间调用

```java
// 玩家加入公会
public void joinGuild(long roleId, long guildId) {
    // 1. 调用 Guild 服务申请加入
    Result<Void> result = RpcTemplate.call(
        () -> guildService.applyJoin(roleId, guildId)
    );
    
    if (result.isSuccess()) {
        // 2. 更新玩家公会信息
        PlayerActor actor = actorSystem.getActor(roleId);
        actor.fire("SET_GUILD", guildId);
        
        // 3. 推送给客户端
        pushService.push(roleId, PushType.GUILD_JOINED, buildGuildInfo());
    }
}

// 玩家升级
@MessageHandler("ADD_EXP")
public void addExp(AddExpData data) {
    long oldExp = getData().getExp();
    long newExp = oldExp + data.getAmount();
    int oldLevel = getData().getLevel();
    
    // 计算是否升级
    while (canLevelUp(getData().getLevel(), newExp)) {
        getData().setLevel(getData().getLevel() + 1);
        newExp -= getLevelExp(getData().getLevel());
    }
    getData().setExp(newExp);
    
    int newLevel = getData().getLevel();
    if (newLevel > oldLevel) {
        // 1. 发布升级事件 (本地)
        eventBus.publish(new PlayerLevelUpEvent(getRoleId(), oldLevel, newLevel));
        
        // 2. 更新排行榜 (调用 Rank 服务)
        rankService.updateScore("level", getRoleId(), newLevel);
        
        // 3. 广播升级消息 (调用 Chat 服务)
        distributedEventBus.publish(new BroadcastEvent(
            "恭喜 " + getData().getRoleName() + " 升到 " + newLevel + " 级！"
        ));
        
        // 4. 推送升级信息给客户端
        pushService.push(getRoleId(), PushType.LEVEL_UP, buildLevelUpPush());
    }
    
    markDirty();
}
```

### 2.4 示例代码结构

```
service-game/src/main/java/com/game/service/game/
├── GameServiceApplication.java
├── handler/
│   ├── PlayerHandler.java          # 玩家协议处理
│   ├── BagHandler.java              # 背包协议处理
│   ├── EquipmentHandler.java        # 装备协议处理
│   └── QuestHandler.java            # 任务协议处理
├── actor/
│   ├── PlayerActor.java             # 玩家Actor (核心)
│   └── PlayerActorSystem.java       # Actor系统管理
├── service/
│   ├── PlayerServiceImpl.java       # 玩家服务 (Dubbo)
│   ├── BagService.java              # 背包服务
│   ├── EquipmentService.java        # 装备服务
│   ├── QuestService.java            # 任务服务
│   └── AttributeService.java        # 属性计算服务
├── event/
│   ├── PlayerLevelUpEvent.java      # 升级事件
│   └── ItemObtainedEvent.java       # 获得物品事件
├── listener/
│   └── PlayerEventListener.java     # 事件监听器
└── config/
    └── PlayerActorConfig.java       # Actor配置
```

---

## 3. service-guild (公会服务)

### 3.1 核心功能

| 功能 | 协议 | 说明 |
|------|------|------|
| 创建公会 | CreateGuild | 创建新公会 |
| 公会信息 | GetGuildInfo | 获取公会详情 |
| 搜索公会 | SearchGuild | 搜索公会列表 |
| 申请加入 | ApplyJoin | 申请加入公会 |
| 审批申请 | HandleApply | 会长审批申请 |
| 退出公会 | LeaveGuild | 主动退出 |
| 踢出成员 | KickMember | 踢出成员 |
| 捐献 | Donate | 公会捐献 |
| 职位变更 | ChangePosition | 修改成员职位 |
| 公会聊天 | - | 通过 Chat 服务实现 |

### 3.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| **Actor模型** | GuildActor + ActorSystem | 公会数据无锁化处理 |
| 协议处理 | @ProtocolController | 处理公会相关协议 |
| 消息处理 | @MessageHandler | Actor 内部消息处理 |
| 事件发布 | EventBus | 成员变动事件 |
| 分布式事件 | DistributedEventBus | 跨服务通知 |
| 推送消息 | PushService | 推送公会变动给所有成员 |
| 分布式锁 | LockService | 创建公会防重复 |
| ID生成 | IdService | 公会ID生成 |
| 缓存 | CacheService | 公会数据缓存 |
| 参数校验 | Validator | 公会名校验 |
| 安全过滤 | SecurityFilter | 公会名敏感词过滤 |
| 排行榜 | RankService | 公会战力排行 |

### 3.3 服务间调用

```java
// 玩家加入公会成功后
@MessageHandler("MEMBER_JOIN")
public void onMemberJoin(MemberJoinData data) {
    // 1. 添加成员
    getData().getMembers().put(data.getRoleId(), new GuildMember(data));
    getData().setMemberCount(getData().getMemberCount() + 1);
    
    // 2. 通知 Game 服务更新玩家公会信息
    RpcTemplate.callAsync(() -> gameService.setPlayerGuild(
        data.getRoleId(), 
        getActorId(), 
        getData().getGuildName()
    ));
    
    // 3. 推送给所有公会成员
    List<Long> memberIds = new ArrayList<>(getData().getMembers().keySet());
    pushService.push(memberIds, PushType.GUILD_MEMBER_CHANGE, 
        buildMemberChangePush(ChangeType.JOIN, data));
    
    // 4. 发布分布式事件 (Chat 服务可监听用于公会聊天)
    distributedEventBus.publish(new GuildMemberChangeEvent(
        getActorId(), data.getRoleId(), ChangeType.JOIN));
    
    markDirty();
}

// 公会捐献
@MessageHandler("DONATE")
public DonateResult donate(DonateData data) {
    // 1. 检查并扣除玩家资源 (调用 Game 服务)
    Result<Long> result = RpcTemplate.call(
        () -> gameService.deductGold(data.getRoleId(), data.getAmount())
    );
    
    if (!result.isSuccess()) {
        return DonateResult.fail(result.getCode());
    }
    
    // 2. 增加公会资金和经验
    long contribution = data.getAmount() / 10;
    getData().setFund(getData().getFund() + data.getAmount());
    getData().setExp(getData().getExp() + contribution);
    
    // 3. 增加成员贡献值
    GuildMember member = getData().getMembers().get(data.getRoleId());
    member.setContribution(member.getContribution() + contribution);
    
    // 4. 检查公会升级
    checkGuildLevelUp();
    
    // 5. 更新公会排行榜
    rankService.updateScore("guild_fund", getActorId(), getData().getFund());
    
    markDirty();
    
    return DonateResult.success(contribution);
}
```

### 3.4 示例代码结构

```
service-guild/src/main/java/com/game/service/guild/
├── GuildServiceApplication.java
├── handler/
│   └── GuildHandler.java            # 公会协议处理
├── actor/
│   ├── GuildActor.java              # 公会Actor (核心)
│   └── GuildActorSystem.java        # Actor系统管理
├── service/
│   └── GuildServiceImpl.java        # 公会服务 (Dubbo)
├── event/
│   └── GuildMemberChangeEvent.java  # 成员变动事件
├── listener/
│   └── GuildEventListener.java      # 事件监听器
└── config/
    └── GuildActorConfig.java        # Actor配置
```

---

## 4. service-chat (聊天服务)

### 4.1 核心功能

| 功能 | 协议 | 说明 |
|------|------|------|
| 发送消息 | SendChat | 发送聊天消息 |
| 获取历史 | GetChatHistory | 获取聊天记录 |
| 世界广播 | - | 系统公告、升级通知 |
| 公会聊天 | - | 公会成员聊天 |
| 私聊 | - | 玩家间私聊 |

### 4.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| 协议处理 | @ProtocolController | 处理聊天相关协议 |
| 分布式事件 | DistributedEventBus | 监听广播事件 |
| 推送消息 | PushService | 推送聊天消息 |
| 安全过滤 | SecurityFilter | 聊天内容敏感词过滤 |
| 限流 | RateLimiterService | 聊天频率限制 |
| 缓存 | CacheService | 聊天记录缓存 |
| Redis Stream | RedisService | 消息队列 |

### 4.3 服务间调用

```java
// 监听分布式广播事件
@EventListener
public void onBroadcast(BroadcastEvent event) {
    // 构建系统消息
    ChatMessage message = ChatMessage.builder()
        .channel(Channel.WORLD)
        .senderType(SenderType.SYSTEM)
        .content(event.getContent())
        .build();
    
    // 保存消息
    chatRepository.save(message);
    
    // 推送给所有在线玩家
    pushService.broadcast(PushType.CHAT_MESSAGE, buildChatPush(message));
}

// 监听公会成员变动事件 (用于公会聊天权限)
@EventListener
public void onGuildMemberChange(GuildMemberChangeEvent event) {
    if (event.getChangeType() == ChangeType.JOIN) {
        // 新成员加入，添加到公会聊天频道
        guildChatChannels.computeIfAbsent(event.getGuildId(), k -> new HashSet<>())
            .add(event.getRoleId());
    } else if (event.getChangeType() == ChangeType.LEAVE) {
        // 成员离开，移除公会聊天权限
        Set<Long> members = guildChatChannels.get(event.getGuildId());
        if (members != null) {
            members.remove(event.getRoleId());
        }
    }
}

// 发送聊天消息
@Protocol(methodId = MethodId.Chat.SEND, desc = "发送聊天")
public Message sendChat(Session session, C2S_SendChat request) {
    long roleId = session.getRoleId();
    
    // 1. 限流检查
    if (!rateLimiter.tryPlayerLimit(roleId, "chat", 5, Duration.ofSeconds(1))) {
        throw new BizException(ErrorCode.RATE_LIMIT);
    }
    
    // 2. 敏感词过滤
    String content = securityFilter.filterSensitiveWords(request.getContent());
    
    // 3. XSS 过滤
    content = securityFilter.filterXss(content);
    
    // 4. 构建消息
    ChatMessage message = buildChatMessage(roleId, request.getChannel(), content);
    
    // 5. 保存消息
    chatRepository.save(message);
    
    // 6. 根据频道推送
    switch (request.getChannel()) {
        case WORLD:
            pushService.broadcast(PushType.CHAT_MESSAGE, buildChatPush(message));
            break;
        case GUILD:
            long guildId = getPlayerGuildId(roleId);
            Set<Long> guildMembers = guildChatChannels.get(guildId);
            pushService.push(guildMembers, PushType.CHAT_MESSAGE, buildChatPush(message));
            break;
        case PRIVATE:
            pushService.push(request.getTargetId(), PushType.CHAT_MESSAGE, buildChatPush(message));
            pushService.push(roleId, PushType.CHAT_MESSAGE, buildChatPush(message));
            break;
    }
    
    return S2C_SendChat.newBuilder()
            .setResult(buildSuccessResult())
            .setMsgId(message.getId())
            .build();
}
```

### 4.4 示例代码结构

```
service-chat/src/main/java/com/game/service/chat/
├── ChatServiceApplication.java
├── handler/
│   └── ChatHandler.java             # 聊天协议处理
├── service/
│   └── ChatServiceImpl.java         # 聊天服务
├── listener/
│   └── ChatEventListener.java       # 事件监听 (广播、公会变动)
├── repository/
│   └── ChatMessageRepository.java   # 消息存储
└── entity/
    └── ChatMessage.java             # 消息实体
```

---

## 5. service-rank (排行服务)

### 5.1 核心功能

| 功能 | 协议 | 说明 |
|------|------|------|
| 获取排行 | GetRankList | 获取排行榜列表 |
| 我的排名 | GetMyRank | 获取自己排名 |
| 更新分数 | - | 内部调用更新分数 |
| 排行奖励 | - | 定时发放排行奖励 |

### 5.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| 协议处理 | @ProtocolController | 处理排行相关协议 |
| **排行榜** | RankService (Redis ZSet) | 排行榜核心功能 |
| 分布式事件 | DistributedEventBus | 监听分数变化事件 |
| 缓存 | CacheService | 玩家信息缓存 |
| RPC调用 | RpcTemplate | 获取玩家详情 |
| 定时任务 | XXL-Job | 排行奖励发放 |

### 5.3 服务间调用

```java
// 监听玩家升级事件更新排行榜
@EventListener
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    rankService.updateScore("level", event.getRoleId(), event.getNewLevel());
}

// 监听战力变化事件
@EventListener  
public void onCombatPowerChange(CombatPowerChangeEvent event) {
    rankService.updateScore("combat_power", event.getRoleId(), event.getNewPower());
}

// 获取排行榜
@Protocol(methodId = MethodId.Rank.GET_LIST, desc = "获取排行榜")
public Message getRankList(Session session, C2S_GetRankList request) {
    String rankType = request.getRankType();
    int start = request.getStart();
    int count = request.getCount();
    
    // 1. 从 Redis 获取排行数据
    List<RankEntry> entries = rankService.getRange(rankType, start, start + count - 1);
    
    // 2. 批量获取玩家详情 (调用 Game 服务)
    List<Long> roleIds = entries.stream().map(RankEntry::getId).toList();
    Map<Long, PlayerBrief> playerBriefs = RpcTemplate.call(
        () -> gameService.getPlayerBriefs(roleIds)
    );
    
    // 3. 组装响应
    List<RankInfo> rankInfos = new ArrayList<>();
    int rank = start;
    for (RankEntry entry : entries) {
        PlayerBrief brief = playerBriefs.get(entry.getId());
        rankInfos.add(RankInfo.newBuilder()
            .setRank(rank++)
            .setRoleId(entry.getId())
            .setRoleName(brief.getRoleName())
            .setLevel(brief.getLevel())
            .setAvatarId(brief.getAvatarId())
            .setScore((long) entry.getScore())
            .build());
    }
    
    return S2C_GetRankList.newBuilder()
            .setResult(buildSuccessResult())
            .setRankType(rankType)
            .addAllRanks(rankInfos)
            .build();
}

// 定时任务: 发放排行奖励
@XxlJob("sendRankRewards")
public void sendRankRewards() {
    log.info("开始发放排行奖励");
    
    // 1. 获取 Top 100
    List<RankEntry> topPlayers = rankService.getTopN("combat_power", 100);
    
    // 2. 根据排名发放奖励 (调用 Game 服务)
    for (int i = 0; i < topPlayers.size(); i++) {
        int rank = i + 1;
        long roleId = topPlayers.get(i).getId();
        List<ItemInfo> rewards = getRankRewards(rank);
        
        RpcTemplate.callAsync(() -> gameService.sendMail(
            roleId,
            "排行奖励",
            "恭喜您获得战力排行第 " + rank + " 名！",
            rewards
        ));
    }
    
    log.info("排行奖励发放完成, 共 {} 人", topPlayers.size());
}
```

### 5.4 示例代码结构

```
service-rank/src/main/java/com/game/service/rank/
├── RankServiceApplication.java
├── handler/
│   └── RankHandler.java             # 排行协议处理
├── service/
│   └── RankServiceImpl.java         # 排行服务
├── listener/
│   └── RankEventListener.java       # 事件监听 (分数变化)
└── job/
    └── RankRewardJob.java           # 排行奖励定时任务
```

---

## 6. service-scheduler (调度服务)

### 6.1 核心功能

| 功能 | 说明 |
|------|------|
| 每日重置 | 每天凌晨重置玩家日常数据 |
| 排行奖励 | 定时发放排行榜奖励 |
| 过期清理 | 清理过期邮件、日志 |
| 在线统计 | 定时统计在线人数 |
| 配置刷新 | 定时检查配置变更 |
| 活动调度 | 活动开启/关闭 |

### 6.2 用到的框架能力

| 能力 | 组件 | 使用场景 |
|------|------|---------|
| 定时任务 | XXL-Job @XxlJob | 任务调度 |
| 分布式事件 | DistributedEventBus | 广播任务事件 |
| RPC调用 | RpcTemplate | 调用各服务执行任务 |
| 分布式锁 | LockService | 防止任务重复执行 |
| 监控指标 | MetricsService | 任务执行统计 |
| 告警通知 | AlertService | 任务失败告警 |
| 配置加载 | ConfigLoader | 触发配置刷新 |

### 6.3 服务间调用

```java
// 每日重置任务
@XxlJob("dailyReset")
public void dailyReset() {
    String lockKey = "job:daily_reset:" + LocalDate.now();
    
    // 分布式锁确保只执行一次
    lockService.executeWithLock(lockKey, () -> {
        log.info("开始执行每日重置");
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 通知 Game 服务重置所有在线玩家
            distributedEventBus.publish(new DailyResetEvent(LocalDate.now()));
            
            // 2. 重置排行榜 (某些类型)
            rankService.reset("daily_dungeon");
            
            // 3. 清理过期数据
            RpcTemplate.callAsync(() -> gameService.cleanExpiredMails());
            
            long costMs = System.currentTimeMillis() - startTime;
            log.info("每日重置完成, 耗时 {}ms", costMs);
            
            // 记录指标
            metrics.recordTime("job.daily_reset", costMs);
            
        } catch (Exception e) {
            log.error("每日重置失败", e);
            // 发送告警
            alertService.alertException("每日重置任务失败", e);
            throw e;
        }
    });
}

// 在线统计任务
@XxlJob("statisticsOnline")
public void statisticsOnline() {
    // 1. 获取各服务在线人数
    int loginOnline = RpcTemplate.callSilent(() -> loginService.getOnlineCount());
    int gameOnline = RpcTemplate.callSilent(() -> gameService.getOnlineCount());
    
    // 2. 记录到监控
    metrics.gauge("online.login", loginOnline);
    metrics.gauge("online.game", gameOnline);
    
    // 3. 保存到数据库 (用于历史统计)
    statisticsRepository.save(new OnlineStatistics(
        LocalDateTime.now(),
        loginOnline,
        gameOnline
    ));
    
    log.info("在线统计: login={}, game={}", loginOnline, gameOnline);
}

// 配置刷新任务
@XxlJob("refreshConfig")  
public void refreshConfig() {
    // 获取配置版本
    String currentVersion = configService.getCurrentVersion();
    String latestVersion = configService.getLatestVersion();
    
    if (!currentVersion.equals(latestVersion)) {
        log.info("检测到配置变更: {} -> {}", currentVersion, latestVersion);
        
        // 触发配置刷新 (会广播到所有服务)
        configLoader.reloadAndBroadcast("all", "scheduler");
        
        // 通知运维
        alertService.notify("运维", "配置已更新", 
            "配置版本: " + currentVersion + " -> " + latestVersion);
    }
}
```

### 6.4 示例代码结构

```
service-scheduler/src/main/java/com/game/service/scheduler/
├── SchedulerServiceApplication.java
├── job/
│   ├── DailyResetJob.java           # 每日重置
│   ├── RankRewardJob.java           # 排行奖励
│   ├── CleanupJob.java              # 数据清理
│   ├── StatisticsJob.java           # 统计任务
│   └── ConfigRefreshJob.java        # 配置刷新
├── config/
│   └── XxlJobConfig.java            # XXL-Job 配置
└── listener/
    └── SchedulerEventListener.java  # 事件监听
```

---

## 框架能力使用汇总

### 各服务使用的框架能力对照表

| 能力 | Login | Game | Guild | Chat | Rank | Scheduler |
|------|:-----:|:----:|:-----:|:----:|:----:|:---------:|
| @ProtocolController | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| Actor模型 | - | ✅ | ✅ | - | - | - |
| @MessageHandler | - | ✅ | ✅ | - | - | - |
| EventBus | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| DistributedEventBus | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| PushService | ✅ | ✅ | ✅ | ✅ | - | - |
| RpcTemplate | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| LockService | ✅ | ✅ | ✅ | - | - | ✅ |
| CacheService | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| RankService | - | ✅ | ✅ | - | ✅ | ✅ |
| TimerService | - | ✅ | - | - | - | - |
| ConfigContainer | - | ✅ | ✅ | - | - | - |
| IdService | ✅ | ✅ | ✅ | ✅ | - | - |
| Validator | ✅ | ✅ | ✅ | ✅ | - | - |
| SecurityFilter | ✅ | - | ✅ | ✅ | - | - |
| RateLimiterService | ✅ | ✅ | - | ✅ | - | - |
| TraceContext | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| MetricsService | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| AlertService | ✅ | ✅ | ✅ | - | - | ✅ |
| ShutdownAware | - | ✅ | ✅ | - | - | - |
| XXL-Job | - | - | - | - | ✅ | ✅ |

---

## 下一步

根据以上设计，需要为每个服务实现完整的示例代码。请确认是否开始实现？

建议实现顺序：
1. **service-game** - 核心服务，包含最完整的框架能力使用
2. **service-guild** - 演示 Actor 间通信和跨服务调用
3. **service-chat** - 演示广播和事件监听
4. **service-rank** - 演示 Redis 排行榜和定时任务
5. **service-scheduler** - 演示分布式任务调度

---

**文档版本**: v1.0  
**最后更新**: 2026-01
