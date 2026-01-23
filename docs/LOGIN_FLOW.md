# 游戏登录流程

> **版本**: v2.0  
> **更新日期**: 2026-01  
> **重要更新**: 握手协议由网关独占处理，登录服务专注账号和角色逻辑

## 流程概览

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  客户端   │────▶│ 1.握手    │────▶│ 2.账号登录 │────▶│ 3.获取服务器│────▶│ 4.选服   │────▶│ 5.创角   │
└──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
                   (Gateway)        (Login)          (Login)          (Login)          (Login)
                                                                                           │
                                                                                           ▼
                                                                                    ┌──────────┐
                                                                                    │ 6.进入游戏 │
                                                                                    └──────────┘
                                                                                       (Login)
```

## 架构说明

### 协议处理分工

| 处理层 | 协议 | 说明 |
|--------|------|------|
| **Gateway** | 握手 (0x0101) | 版本校验、公告获取、Session 初始化 |
| **Gateway** | 心跳 (0x010B) | 连接保活、延迟检测 |
| **Login Service** | 账号登录 ~ 进入游戏 | 完整的账号和角色业务逻辑 |

> ⚠️ **重要**: 握手协议仅由 Gateway 的 `GatewayMessageHandler` 处理，Login Service 不再处理握手，避免协议路由冲突。

## 协议号规划

使用 `MethodId` 常量类统一管理，避免硬编码。

| 阶段 | 协议号 | 常量名 | 处理位置 | 描述 |
|-----|--------|--------|----------|------|
| 1 | 0x0101 | `MethodId.Login.HANDSHAKE` | **Gateway** | 握手 |
| 2 | 0x0102 | `MethodId.Login.ACCOUNT_LOGIN` | Login Service | 账号登录 |
| 2 | 0x0103 | `MethodId.Login.BIND_ACCOUNT` | Login Service | 绑定账号 |
| 3 | 0x0104 | `MethodId.Login.GET_SERVER_LIST` | Login Service | 获取服务器列表 |
| 4 | 0x0105 | `MethodId.Login.SELECT_SERVER` | Login Service | 选择服务器 |
| 5 | 0x0106 | `MethodId.Login.CHECK_ROLE_NAME` | Login Service | 检查角色名 |
| 5 | 0x0107 | `MethodId.Login.CREATE_ROLE` | Login Service | 创建角色 |
| 6 | 0x0108 | `MethodId.Login.ENTER_GAME` | Login Service | 进入游戏 |
| - | 0x0109 | `MethodId.Login.LOGOUT` | Login Service | 登出 |
| - | 0x010A | `MethodId.Login.RECONNECT` | Login Service | 重连 |
| - | 0x010B | `MethodId.Login.HEARTBEAT` | **Gateway** | 心跳 |
| - | 0x010C | - | Gateway Push | 踢下线推送 |

### MethodId 常量定义

```java
// common/common-api/src/main/java/com/game/api/common/MethodId.java
public final class MethodId {
    public static final class Login {
        public static final int HANDSHAKE = 0x01;
        public static final int ACCOUNT_LOGIN = 0x02;
        public static final int BIND_ACCOUNT = 0x03;
        public static final int GET_SERVER_LIST = 0x04;
        public static final int SELECT_SERVER = 0x05;
        public static final int CHECK_ROLE_NAME = 0x06;
        public static final int CREATE_ROLE = 0x07;
        public static final int ENTER_GAME = 0x08;
        public static final int LOGOUT = 0x09;
        public static final int RECONNECT = 0x0A;
        public static final int HEARTBEAT = 0x0B;
    }
}
```

## 详细流程

### 第一阶段：握手 (Handshake) - Gateway 处理

客户端连接网关后立即发送握手请求，**由 Gateway 独占处理**。

**处理器**: `GatewayMessageHandler.handleHandshake()`

```protobuf
// game.proto
message C2S_Handshake {
    string client_version = 1;    // 客户端版本号
    string platform = 2;          // 平台 (ios/android/pc)
    string device_id = 3;         // 设备唯一标识
    string device_model = 4;      // 设备型号
    string os_version = 5;        // 系统版本
    string language = 6;          // 语言
    string channel = 7;           // 渠道
}

message S2C_Handshake {
    Result result = 1;
    int64 server_time = 2;        // 服务器时间
    string session_key = 3;       // 会话密钥 (用于重连)
    bool need_update = 4;         // 是否需要强制更新
    string update_url = 5;        // 更新地址
    string notice = 6;            // 公告内容
}
```

### 第二阶段：账号登录 (AccountLogin) - Login Service 处理

支持多种登录方式：

| LoginType | 值 | 说明 | credential 内容 |
|-----------|---|------|-----------------|
| GUEST | 0 | 游客登录 | 空 |
| ACCOUNT | 1 | 账号密码 | password (MD5) |
| GOOGLE | 2 | Google 登录 | Google Access Token |
| FACEBOOK | 3 | Facebook 登录 | Facebook Access Token |
| APPLE | 4 | Apple 登录 | Apple Token |
| TOKEN | 5 | Token 自动登录 | 上次登录的 Token |

```protobuf
message C2S_AccountLogin {
    LoginType login_type = 1;     // 登录类型
    string account = 2;           // 账号 (游客时为设备ID)
    string credential = 3;        // 凭证 (密码/第三方Token)
    string device_id = 4;         // 设备 ID
    string platform = 5;          // 平台
    string client_version = 6;    // 客户端版本
    string channel = 7;           // 渠道
}

message S2C_AccountLogin {
    Result result = 1;
    string account_id = 2;        // 账号 ID
    string token = 3;             // 登录 Token
    int64 token_expire_time = 4;  // Token 过期时间
    bool is_new_account = 5;      // 是否新注册账号
}
```

**登录逻辑流程：**

```
                     ┌─────────────┐
                     │  登录请求   │
                     └──────┬──────┘
                            ▼
              ┌─────────────────────────┐
              │    判断 LoginType       │
              └─────────────────────────┘
                   │    │    │    │
         ┌─────────┼────┼────┼────┼─────────┐
         ▼         ▼    ▼    ▼    ▼         ▼
      GUEST    ACCOUNT  G   FB  APPLE    TOKEN
         │         │    │    │    │         │
         ▼         ▼    ▼    ▼    ▼         ▼
    ┌────────┐ ┌──────┐ └──┬──┘    │    ┌───────┐
    │按设备ID │ │验证  │    ▼       │    │验证   │
    │查找账号│ │密码  │ ┌──────┐   │    │Token  │
    └───┬────┘ └──┬───┘ │第三方│   │    └───┬───┘
        │         │     │验证  │   │        │
        ▼         ▼     └──┬───┘   │        ▼
    ┌────────────────────────────────────────┐
    │          查找/创建账号                  │
    └────────────────────────────────────────┘
                         │
                         ▼
                  ┌────────────┐
                  │ 生成Token  │
                  │ 返回响应   │
                  └────────────┘
```

### 第三阶段：获取服务器列表 (GetServerList)

登录成功后，客户端获取可进入的服务器列表。

```protobuf
message C2S_GetServerList {
    // 无需额外参数，使用 Session 中的 accountId
}

message S2C_GetServerList {
    Result result = 1;
    repeated ServerGroup server_groups = 2;  // 服务器分组
    int32 recommended_server_id = 3;         // 推荐服务器
    int32 last_login_server_id = 4;          // 上次登录服务器
}

message ServerGroup {
    int32 group_id = 1;           // 分组 ID
    string group_name = 2;        // 分组名 (推荐/最新/经典)
    repeated ServerInfo servers = 3;
}

message ServerInfo {
    int32 server_id = 1;
    string server_name = 2;
    int32 status = 3;             // 0:维护 1:流畅 2:繁忙 3:爆满 4:新服
    int64 open_time = 4;          // 开服时间
    string server_tag = 5;        // 标签
}
```

### 第四阶段：选择服务器 (SelectServer)

选择服务器后获取该服角色列表。

```protobuf
message C2S_SelectServer {
    int32 server_id = 1;
}

message S2C_SelectServer {
    Result result = 1;
    string game_server_host = 2;  // 游戏服地址 (分布式部署时)
    int32 game_server_port = 3;
    string enter_token = 4;       // 进入游戏 Token
    repeated RoleBrief roles = 5; // 该服角色列表
}
```

### 第五阶段：创建角色 (CreateRole)

如果该服务器没有角色，需要先创建。

```protobuf
message C2S_CheckRoleName {
    string role_name = 1;
}

message S2C_CheckRoleName {
    Result result = 1;
    bool available = 2;           // 是否可用
    string suggestion = 3;        // 推荐名字
}

message C2S_CreateRole {
    string role_name = 1;
    int32 avatar_id = 2;          // 头像
    int32 gender = 3;             // 性别 (0:女 1:男)
    int32 profession = 4;         // 职业
}

message S2C_CreateRole {
    Result result = 1;
    RoleBrief role = 2;
}
```

### 第六阶段：进入游戏 (EnterGame)

选择角色进入游戏，下发完整玩家数据。

```protobuf
message C2S_EnterGame {
    int64 role_id = 1;
}

message S2C_EnterGame {
    Result result = 1;
    PlayerFullInfo player = 2;    // 玩家完整信息
    int64 server_time = 3;        // 服务器时间
}

message PlayerFullInfo {
    int64 role_id = 1;
    string role_name = 2;
    int32 level = 3;
    int64 exp = 4;
    int64 gold = 5;
    int64 diamond = 6;
    int64 bind_diamond = 7;
    int32 vip_level = 8;
    int32 vip_exp = 9;
    int32 avatar_id = 10;
    int32 frame_id = 11;
    int32 gender = 12;
    int32 profession = 13;
    int32 energy = 14;
    int32 max_energy = 15;
    int64 energy_recover_time = 16;
    int64 combat_power = 17;
    int64 create_time = 18;
    int64 guild_id = 19;
    string guild_name = 20;
    int32 guild_position = 21;
    string signature = 22;
}
```

## 重连流程

断线后客户端可使用 Token 快速重连：

```
客户端 ──(携带Token+RoleId)──▶ 网关 ──▶ Login Service ──▶ 验证Token ──▶ 恢复Session ──▶ 返回玩家数据
```

```protobuf
message C2S_Reconnect {
    string token = 1;
    int64 role_id = 2;
    int32 server_id = 3;
}

message S2C_Reconnect {
    Result result = 1;
    PlayerFullInfo player = 2;
    int64 server_time = 3;
}
```

## 服务端实现

### Gateway 处理器

```java
// gateway/gateway-server/.../GatewayMessageHandler.java
@Component
public class GatewayMessageHandler {
    
    /**
     * 握手请求 - Gateway 独占处理
     */
    @Protocol(value = 0x0101, desc = "握手", requireLogin = false)
    public S2C_Handshake handleHandshake(Session session, GameMessage message) {
        C2S_Handshake request = C2S_Handshake.parseFrom(message.getBody());
        
        // 版本检查、公告获取、Session 初始化
        session.setAttribute("deviceId", request.getDeviceId());
        session.setAttribute("platform", request.getPlatform());
        
        return S2C_Handshake.newBuilder()
                .setResult(buildSuccessResult())
                .setServerTime(System.currentTimeMillis())
                .setSessionKey(session.getReconnectToken())
                .build();
    }
    
    /**
     * 心跳请求 - Gateway 独占处理
     */
    @Protocol(value = ProtocolConstants.HEARTBEAT_REQ, desc = "心跳", requireLogin = false)
    public S2C_Heartbeat handleHeartbeat(Session session, GameMessage message) {
        session.updateActiveTime();
        return S2C_Heartbeat.newBuilder()
                .setResult(buildSuccessResult())
                .setServerTime(System.currentTimeMillis())
                .build();
    }
}
```

### Login Service 处理器

```java
// services/service-login/.../LoginHandler.java
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_LOGIN, value = "登录模块")
public class LoginHandler extends BaseHandler {
    
    // 注意: 握手协议由 Gateway 独占处理 (GatewayMessageHandler.handleHandshake)
    // 这里不再重复注册握手处理器，避免协议路由冲突
    
    @Protocol(methodId = MethodId.Login.ACCOUNT_LOGIN, desc = "账号登录", 
              requireLogin = false, requireRole = false)
    public Message accountLogin(Session session, C2S_AccountLogin request) {
        // 账号登录逻辑...
    }
    
    @Protocol(methodId = MethodId.Login.GET_SERVER_LIST, desc = "获取服务器列表", 
              requireLogin = true, requireRole = false)
    public Message getServerList(Session session, C2S_GetServerList request) {
        // 获取服务器列表逻辑...
    }
    
    @Protocol(methodId = MethodId.Login.CREATE_ROLE, desc = "创建角色", 
              requireLogin = true, requireRole = false)
    public Message createRole(Session session, C2S_CreateRole request) {
        // 创建角色逻辑...
    }
    
    @Protocol(methodId = MethodId.Login.ENTER_GAME, desc = "进入游戏", 
              requireLogin = true, requireRole = false)
    public Message enterGame(Session session, C2S_EnterGame request) {
        // 进入游戏逻辑...
    }
}
```

## 错误处理

### 推荐方式：抛出 BizException

```java
// 错误处理由框架 ProtocolDispatcher 统一完成
// Handler 中直接抛出异常，无需返回特定错误响应类型

@Protocol(methodId = MethodId.Player.GET_INFO, desc = "获取玩家信息")
public Message getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
    PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
    if (actor == null) {
        // ✅ 正确：抛出异常，框架自动构建正确的错误响应
        throw new BizException(ErrorCode.ROLE_NOT_FOUND);
    }
    
    // ❌ 错误：不要手动构建错误响应，可能导致消息类型不匹配
    // return S2C_GetPlayerInfo.newBuilder()
    //         .setResult(buildErrorResult(ErrorCode.ROLE_NOT_FOUND))
    //         .build();
    
    return S2C_GetPlayerInfo.newBuilder()
            .setResult(buildSuccessResult())
            .setPlayer(buildPlayerInfo(actor.getData()))
            .build();
}
```

### 错误码

| 错误码 | 常量 | 说明 |
|--------|------|------|
| 2001 | ACCOUNT_NOT_FOUND | 账号不存在 |
| 2003 | PASSWORD_ERROR | 密码错误 |
| 2004 | TOKEN_INVALID | Token 无效 |
| 2005 | TOKEN_EXPIRED | Token 已过期 |
| 2006 | ACCOUNT_BANNED | 账号已封禁 |
| 2009 | ROLE_NAME_EXISTS | 角色名已存在 |
| 2010 | ROLE_NAME_INVALID | 角色名不合法 |
| 2011 | SERVER_MAINTENANCE | 服务器维护中 |
| 2017 | ACCOUNT_EXISTS | 账号已存在 |
| 2018 | THIRD_PARTY_AUTH_FAILED | 第三方登录验证失败 |
| 2019 | THIRD_PARTY_ALREADY_BOUND | 第三方账号已被绑定 |
| 2020 | SERVER_NOT_FOUND | 服务器不存在 |
| 2021 | SERVER_FULL | 服务器已满 |
| 2022 | VERSION_TOO_LOW | 版本过低 |

## 客户端实现参考

```typescript
class LoginManager {
    private token: string = '';
    
    // 完整登录流程
    async fullLogin(loginType: LoginType, credential: string): Promise<void> {
        // 1. 握手 (由 Gateway 处理)
        const handshakeResult = await this.handshake();
        if (handshakeResult.needUpdate) {
            this.redirectToUpdate(handshakeResult.updateUrl);
            return;
        }
        
        // 2. 账号登录 (由 Login Service 处理)
        const loginResult = await this.accountLogin(loginType, credential);
        this.token = loginResult.token;
        
        // 3. 获取服务器列表
        const serverList = await this.getServerList();
        
        // 4. 选择服务器 (UI 交互)
        const serverId = await this.showServerSelectUI(serverList);
        const selectResult = await this.selectServer(serverId);
        
        // 5. 检查是否有角色
        if (selectResult.roles.length === 0) {
            // 创建角色 (UI 交互)
            const roleInfo = await this.showCreateRoleUI();
            await this.createRole(roleInfo);
        }
        
        // 6. 进入游戏
        const roleId = selectResult.roles[0].roleId;
        const enterResult = await this.enterGame(roleId);
        
        // 进入游戏场景
        this.enterGameScene(enterResult.player);
    }
    
    // 重连流程
    async reconnect(): Promise<boolean> {
        if (!this.token || !this.lastRoleId) {
            return false;
        }
        
        try {
            const result = await this.sendReconnect(this.token, this.lastRoleId);
            if (result.success) {
                this.enterGameScene(result.player);
                return true;
            }
        } catch (e) {
            console.error('Reconnect failed:', e);
        }
        
        return false;
    }
}
```

## 安全考虑

1. **Token 安全**：Token 使用 UUID + 时间戳生成，Redis 存储，7 天过期
2. **密码安全**：客户端 MD5 + 服务端 BCrypt 双重加密
3. **第三方登录**：验证第三方平台返回的 Access Token
4. **设备绑定**：记录登录设备，异常登录可触发安全验证
5. **频率限制**：登录接口添加频率限制，防止暴力破解
6. **请求验证**：使用 `RequestValidator` 检查时间戳和签名，防止重放攻击

---

**文档版本**: v2.0  
**最后更新**: 2026-01