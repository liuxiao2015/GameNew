# Game Server Framework

分布式微服务游戏服务器框架 - 基于 Actor 模型的无锁化设计

## 技术栈

| 技术 | 版本 | 用途 |
|-----|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.3.5 | 应用框架 |
| Dubbo | 3.2.9 | RPC 框架 |
| Nacos | 2.3.0 | 注册/配置中心 |
| MongoDB | 7.0+ | 持久化存储 |
| Redis | 7.2+ | 缓存/消息 |
| RabbitMQ | 3.12+ | 消息队列 |
| Netty | 4.1.108 | 网络通信 |
| Protobuf | 3.25.1 | 客户端协议 |

## 项目结构

```
game-server-framework/
├── launcher/                     # 服务启动器 (统一管理所有服务)
├── framework/                    # 框架层
│   ├── framework-common/         # 公共组件
│   ├── framework-core/           # 核心框架
│   ├── framework-actor/          # Actor 框架
│   ├── framework-data/           # 数据访问层
│   ├── framework-mq/             # 消息队列框架
│   └── framework-log/            # 日志组件
├── common/                       # 公共模块
│   ├── common-api/               # 服务接口定义 + Proto
│   ├── common-entity/            # 公共实体/仓库
│   └── common-config/            # 公共配置
├── services/                     # 业务服务层
│   ├── service-gateway/          # 网关服务
│   ├── service-game/             # 游戏服务 (PlayerActor)
│   ├── service-guild/            # 公会服务 (GuildActor)
│   ├── service-login/            # 登录服务
│   ├── service-chat/             # 聊天服务
│   ├── service-rank/             # 排行服务
│   ├── service-scheduler/        # 定时任务服务
│   ├── service-activity/         # 活动服务
│   ├── service-pay/              # 支付服务
│   ├── service-battle/           # 战斗服务
│   └── service-gm/               # GM 运营后台
├── docker/                       # Docker 配置
├── tools/                        # 工具 (包含 Nacos 安装包)
└── docs/                         # 文档
```

## 快速开始

### 环境准备

- JDK 21+
- Maven 3.9+
- Docker (可选，推荐用于生产环境)

---

## 🚀 一键启动

### Windows 本地开发 (推荐)

无需 Docker，启动器会自动管理 Nacos：

```powershell
# 方式一：使用脚本
.\scripts\start-local.bat

# 方式二：直接使用 Launcher
mvn package -DskipTests -pl launcher -am
java -jar launcher\target\launcher-1.0.0-SNAPSHOT.jar up --local
```

> **注意**：需要手动安装 [MongoDB](https://www.mongodb.com/try/download/community) 和 [Redis](https://github.com/tporadowski/redis/releases)

### Docker 环境

```bash
# 编译并启动
mvn package -DskipTests -pl launcher -am
java -jar launcher/target/launcher-1.0.0-SNAPSHOT.jar up
```

---

## Launcher 命令参考

```bash
# 进入交互模式
java -jar launcher/target/launcher-1.0.0-SNAPSHOT.jar

# 可用命令
up                    # 一键启动 (自动检测 Docker/本地)
up --local            # 本地模式 (自动启动 Nacos)
up -f                 # 跳过基础设施检查
down                  # 停止所有服务
down all              # 停止所有服务 + 基础设施

# 服务管理
start all             # 启动所有服务
start service-game    # 启动指定服务
stop service-chat     # 停止指定服务
restart all           # 重启所有服务
status                # 查看服务状态
list                  # 列出所有服务

# Nacos 本地管理
nacos start           # 启动本地 Nacos
nacos stop            # 停止本地 Nacos
nacos status          # 查看 Nacos 状态

# Docker 管理
docker start          # 启动 Docker 基础设施
docker stop           # 停止 Docker 基础设施
docker status         # 查看容器状态

# 其他
check                 # 检查基础设施状态
logs service-game 50  # 查看日志
help                  # 显示帮助
```

---

## 服务访问地址

| 服务 | 地址 | 说明 |
|-----|------|------|
| Nacos | http://localhost:8848/nacos | 账号: nacos/nacos |
| GM后台 | http://localhost:8090 | Swagger: /swagger-ui.html |
| Grafana | http://localhost:3000 | 日志查看 (Docker) |
| MongoDB | localhost:27017 | 数据库 |
| Redis | localhost:6379 | 缓存 |
| RabbitMQ | localhost:15672 | 账号: guest/guest |

---

## 核心特性

### Actor 模型无锁化

- 每个玩家/公会对应一个 Actor 实例
- Actor 内部单线程顺序处理消息
- 业务层无需考虑并发问题

### 三层数据架构

```
Actor 内存 → Redis 缓存 → MongoDB 持久化
```

- Actor 内存: 热数据，毫秒级响应
- Redis 缓存: 温数据，跨服务共享
- MongoDB 持久化: 冷数据，最终一致

### 消息队列

- 使用 RabbitMQ 替代 Redis Pub/Sub
- 支持聊天消息、分布式事件、战斗同步

### 协议设计

使用 Protobuf 定义客户端协议：
- 协议文件位置: `common/common-api/src/main/proto/`
- 协议号规划: 1000-登录, 2000-玩家, 6000-公会, 7000-聊天, 8000-排行

---

## 开发规范

- 使用中文注释
- Java 21 特性: Record, Pattern Matching, Virtual Threads
- 代码风格: 4空格缩进, 120字符行长
- 依赖注入: 构造器注入
- 异常处理: GameException + 全局异常处理器

## 文档

- [架构设计文档](docs/架构设计/架构设计文档-v3.0.md)

## License

Private - All Rights Reserved
