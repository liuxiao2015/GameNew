# 游戏服务器框架文档中心

> **版本**: v2.0  
> **最后更新**: 2026-02  
> **设计理念**: 开发者只需关注业务逻辑，框架处理一切底层细节

---

## 📚 文档目录

### 快速入门

| 文档 | 说明 |
|-----|------|
| [框架能力总览](框架拆解/框架能力总览.md) | 框架所有能力的概览和使用示例 |
| [服务开发指南](框架拆解/服务开发指南.md) | 各服务模块的开发指南和最佳实践 |

### 核心功能

| 文档 | 说明 |
|-----|------|
| [完整登录流程](框架拆解/完整登录流程.md) | 从客户端连接到进入游戏的完整流程 |
| [协议常量定义](框架拆解/协议常量定义.md) | 协议号、方法号等常量定义规范 |
| [广播与推送](框架拆解/广播与推送.md) | 基于 Dubbo 的消息推送机制 |

### 模块使用指南 ⭐

| 文档 | 说明 |
|-----|------|
| [数据层使用指南](./模块使用指南/数据层使用指南.md) | MongoDB + Redis 数据访问 |
| [Actor模型使用指南](./模块使用指南/Actor模型使用指南.md) | 无锁化并发模型，实体状态管理 |
| [定时任务使用指南](./模块使用指南/定时任务使用指南.md) | XXL-Job 分布式任务调度 |
| [DubboRPC使用指南](./模块使用指南/DubboRPC使用指南.md) | 微服务间 RPC 调用 |
| [事件总线使用指南](./模块使用指南/事件总线使用指南.md) | 模块解耦、异步通信 |
| [配置加载使用指南](./模块使用指南/配置加载使用指南.md) | 游戏配置表加载与热更新 |

### 进阶功能

| 文档 | 说明 |
|-----|------|
| [热更新框架](框架拆解/热更新框架.md) | JSON 配置热更新和 Groovy 代码热更新 |
| [数据一致性与链路追踪](框架拆解/数据一致性与链路追踪.md) | 跨服务数据一致性和分布式追踪 |
| [Dubbo优化](框架拆解/Dubbo优化.md) | Dubbo 服务调用和分布式优化 |

### 架构设计

| 文档 | 说明 |
|-----|------|
| [架构设计文档](./架构设计/架构设计文档-v3.1.md) | 整体架构设计和模块划分 |
| [框架搭建执行计划](./架构设计/框架搭建执行计划-v3.0.md) | 框架搭建的详细执行计划 |

### 示例代码

| 文档 | 说明 |
|-----|------|
| [框架使用示例](./示例代码/框架使用示例.java) | 框架各能力的使用示例 |
| [Handler开发示例](./示例代码/Handler开发示例.java) | 协议处理器开发示例 |

---

## 🚀 快速启动

### 一键启动 (推荐)

```bash
# Windows
.\scripts\start-services.ps1 up

# Linux/Mac
./scripts/start-services.sh up
```

### 交互式管理

```bash
# 进入交互模式
.\scripts\start-services.ps1

# 常用命令
launcher> up          # 一键启动 (Docker + 所有服务)
launcher> status      # 查看服务状态
launcher> list        # 列出所有服务
launcher> down        # 停止所有服务
launcher> help        # 显示帮助
```

### 访问地址

| 服务 | 地址 |
|------|------|
| Nacos 控制台 | http://localhost:8848/nacos |
| Grafana 监控 | http://localhost:3000 |
| GM 后台 Swagger | http://localhost:8090/swagger-ui.html |
| RabbitMQ 管理 | http://localhost:15672 |

---

## 📖 快速上手

### 1. 了解框架能力

首先阅读 [框架能力总览](框架拆解/框架能力总览.md)，了解框架提供的所有能力。

### 2. 学习核心模块

按照以下顺序学习各核心模块：

1. **数据层**: [数据层使用指南](./模块使用指南/数据层使用指南.md) - MongoDB 和 Redis 的使用
2. **Actor模型**: [Actor模型使用指南](./模块使用指南/Actor模型使用指南.md) - 玩家/公会等实体管理
3. **RPC调用**: [DubboRPC使用指南](./模块使用指南/DubboRPC使用指南.md) - 服务间通信
4. **事件总线**: [事件总线使用指南](./模块使用指南/事件总线使用指南.md) - 模块解耦
5. **定时任务**: [定时任务使用指南](./模块使用指南/定时任务使用指南.md) - XXL-Job 使用
6. **配置加载**: [配置加载使用指南](./模块使用指南/配置加载使用指南.md) - 策划表加载

### 3. 查看登录流程

阅读 [完整登录流程](框架拆解/完整登录流程.md)，理解客户端如何连接和登录游戏。

### 4. 开发业务服务

参考 [服务开发指南](框架拆解/服务开发指南.md)，按照模板开发各业务服务。

### 5. 进阶功能

根据需要阅读热更新、数据一致性等进阶文档。

---

## 📁 项目目录结构

```
game-server/
├── common/                      # 通用模块
│   ├── common-api/              # Dubbo 服务接口和 DTO
│   ├── common-entity/           # MongoDB 实体和 Repository
│   └── common-config/           # 游戏配置表
│
├── framework/                   # 框架模块
│   ├── framework-common/        # 公共工具类
│   ├── framework-core/          # 核心框架
│   ├── framework-actor/         # Actor 并发模型
│   ├── framework-data/          # 数据访问 (MongoDB/Redis)
│   ├── framework-log/           # 日志模块
│   └── framework-mq/            # 消息队列 (RabbitMQ)
│
├── services/                    # 微服务 (所有可启动的服务)
│   ├── service-gateway/         # 网关服务
│   ├── service-login/           # 登录服务
│   ├── service-game/            # 游戏服务
│   ├── service-guild/           # 公会服务
│   ├── service-chat/            # 聊天服务
│   ├── service-rank/            # 排行榜服务
│   ├── service-scheduler/       # 定时任务服务
│   ├── service-activity/        # 活动服务
│   ├── service-pay/             # 支付服务
│   ├── service-battle/          # 战斗服务
│   └── service-gm/              # GM 后台服务
│
├── launcher/                    # 服务启动器 (独立模块)
│
├── docker/                      # Docker 配置
│   ├── docker-compose.yml
│   ├── loki/                    # Loki 日志
│   └── grafana/                 # Grafana 监控
│
├── scripts/                     # 启动脚本
│
├── docs/                        # 文档
│   ├── 架构设计/
│   ├── 框架拆解/
│   ├── 模块使用指南/
│   └── 示例代码/
│
└── launcher.yaml                # 启动器配置
```

---

## 🔧 开发规范

### 协议开发

1. 使用 `@ProtocolController` 标注模块
2. 使用 `@Protocol` 标注方法
3. 使用 `MethodId` 常量定义方法号
4. 抛出 `BizException` 处理业务错误

### 服务开发

1. 继承 `BaseService` 获取便捷方法
2. 使用 `@EventListener` 订阅事件
3. 使用 `DubboPushService` 推送消息
4. 实现 `ShutdownAware` 支持优雅停机

### 配置开发

1. 继承 `BaseConfigContainer` 创建配置容器
2. 使用 `@ConfigContainer` 注解标注
3. 配置类实现 `GameConfig` 接口

### Actor 开发

1. 继承 `Actor<T>` 创建 Actor 类
2. 实现 `loadData()` 和 `saveData()` 方法
3. 通过 `ActorSystem` 管理 Actor 生命周期
4. 使用 `markDirty()` 标记数据需要保存

### RPC 开发

1. 在 `common-api` 定义接口
2. 使用 `@DubboService` 暴露服务
3. 使用 `@DubboReference` 引用服务
4. 选择合适的负载均衡策略

---

## 📊 模块速查表

| 需求 | 使用模块 | 关键类/注解 |
|-----|---------|------------|
| 持久化数据 | MongoDB | `BaseRepository`, `MongoTemplate` |
| 缓存数据 | Redis | `RedisService` |
| 在线玩家状态 | Actor | `Actor`, `ActorSystem` |
| 服务间调用 | Dubbo | `@DubboService`, `@DubboReference` |
| 模块解耦 | EventBus | `@EventListener`, `eventBus.publish()` |
| 定时任务 | XXL-Job | `@XxlJob` |
| 游戏配置 | ConfigLoader | `@ConfigContainer`, `GameConfig` |
| 消息推送 | Push | `DubboPushService` |
| 分布式锁 | Redis | `redisService.tryLock()` |
| 排行榜 | Redis ZSet | `redisService.zAdd()` |

---

## 📞 联系方式

如有问题，请联系框架开发团队。
