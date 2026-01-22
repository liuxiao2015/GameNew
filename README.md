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
| Netty | 4.1.108 | 网络通信 |
| Protobuf | 3.25.1 | 客户端协议 |
| XXL-Job | 2.4.0 | 定时任务 |

## 项目结构

```
game-server-framework/
├── framework/                    # 框架层
│   ├── framework-common/         # 公共组件
│   ├── framework-core/           # 核心框架
│   ├── framework-actor/          # Actor 框架
│   ├── framework-data/           # 数据访问层
│   └── framework-log/            # 日志组件
├── gateway/                      # 网关层
│   └── gateway-server/           # 网关服务
├── services/                     # 业务服务层
│   ├── service-api/              # 服务接口定义 + Proto
│   ├── service-game/             # 游戏服务 (PlayerActor)
│   ├── service-guild/            # 公会服务 (GuildActor)
│   ├── service-login/            # 登录服务
│   ├── service-chat/             # 聊天服务
│   ├── service-rank/             # 排行服务
│   ├── service-task/             # 定时任务服务
│   └── service-gm/               # GM 运营后台
├── docker/                       # Docker 配置
│   ├── docker-compose.yml        # 本地开发环境
│   └── init/                     # 初始化脚本
└── docs/                         # 文档
    └── architecture/             # 架构文档
```

## 快速开始

### 1. 环境准备

- JDK 21+
- Maven 3.9+
- Docker & Docker Compose (用于本地开发环境)

### 启动脚本 (推荐)

```bash
# Windows PowerShell
.\scripts\start-infra.ps1       # 启动基础设施
.\scripts\import-nacos-config.ps1  # 导入 Nacos 配置
.\scripts\build.ps1             # 编译项目
.\scripts\start-services.ps1    # 启动所有服务
.\scripts\stop-services.ps1     # 停止所有服务

# Linux/macOS
./scripts/start-infra.sh        # 启动基础设施
./scripts/import-nacos-config.sh   # 导入 Nacos 配置
./scripts/build.sh              # 编译项目
./scripts/start-services.sh     # 启动所有服务
./scripts/stop-services.sh      # 停止所有服务
```

### 2. 启动基础设施

**方式一：使用脚本（推荐）**

```powershell
# Windows PowerShell
.\scripts\start-infra.ps1
```

```bash
# Linux/Mac
./scripts/start-infra.sh
```

**方式二：手动启动**

```bash
# 进入 docker 目录
cd docker

# 启动 MongoDB, Redis, Nacos, XXL-Job
docker-compose up -d

# 查看服务状态
docker-compose ps
```

服务访问地址：
- Nacos: http://localhost:8848/nacos (账号: nacos/nacos)
- XXL-Job: http://localhost:8088/xxl-job-admin (账号: admin/123456)
- MongoDB: localhost:27017
- Redis: localhost:6379

### 3. 编译项目

**方式一：使用脚本**

```powershell
# Windows PowerShell
.\scripts\build.ps1 -SkipTests
```

**方式二：Maven 命令**

```bash
# 编译所有模块
mvn clean install -DskipTests

# 编译 Protobuf (service-api 模块会自动编译)
```

### 4. 启动服务

**方式一：使用脚本**

```powershell
# 启动所有服务
.\scripts\start-services.ps1

# 启动单个服务
.\scripts\start-services.ps1 -Service gateway
.\scripts\start-services.ps1 -Service game
```

**方式二：Maven 命令（按顺序启动）**

```bash
# 1. 登录服务
cd services/service-login && mvn spring-boot:run

# 2. 游戏服务
cd services/service-game && mvn spring-boot:run

# 3. 公会服务
cd services/service-guild && mvn spring-boot:run

# 4. 聊天服务
cd services/service-chat && mvn spring-boot:run

# 5. 排行服务
cd services/service-rank && mvn spring-boot:run

# 6. 任务服务
cd services/service-task && mvn spring-boot:run

# 7. GM 后台
cd services/service-gm && mvn spring-boot:run

# 8. 网关服务
cd gateway/gateway-server && mvn spring-boot:run
```

### 5. 验证

```bash
# 检查 Nacos 服务列表
curl http://localhost:8848/nacos/v1/ns/service/list

# 网关 TCP 端口
telnet localhost 9000
```

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

### 协议设计

使用 Protobuf 定义客户端协议：
- 协议文件位置: `services/service-api/src/main/proto/`
- 协议号规划: 1000-登录, 2000-玩家, 6000-公会, 7000-聊天, 8000-排行

## 开发规范

- 使用中文注释
- Java 17+ 特性: Record, Pattern Matching, Text Blocks
- 代码风格: 4空格缩进, 120字符行长
- 依赖注入: 构造器注入
- 异常处理: GameException + 全局异常处理器

## 文档

- [架构设计文档](docs/architecture/架构设计文档-v3.0.md)
- [框架搭建执行计划](docs/architecture/框架搭建执行计划-v3.0.md)

## License

Private - All Rights Reserved
