# Proto 协议生成工具

## 文件结构

```
tools/proto/
├── README.md           # 本文档
├── generate_proto.bat  # Windows 生成脚本
├── generate_proto.sh   # Linux/macOS 生成脚本
└── output/             # 生成输出目录
    ├── java/
    ├── ts/
    ├── js/
    └── go/

common/common-api/src/main/proto/
└── game.proto          # 唯一的协议定义文件
```

## 快速开始

### 后端 (Java)

Java 代码由 Maven 自动生成，执行编译即可：

```bash
mvn compile -pl common/common-api -am
```

生成的 Java 类位于：
```
common/common-api/target/generated-sources/protobuf/
```

### 前端 (TypeScript/JavaScript)

#### 方法1: 使用脚本生成

```bash
# Windows
tools\proto\generate_proto.bat ts

# Linux/macOS
./tools/proto/generate_proto.sh ts
```

#### 方法2: 手动生成

```bash
# 安装依赖
npm install -g protobufjs

# 生成 JavaScript (ES6 模块)
npx pbjs -t static-module -w es6 -o game.js common/common-api/src/main/proto/game.proto

# 生成 TypeScript 类型定义
npx pbts -o game.d.ts game.js

# 生成 JSON 描述文件 (轻量级)
npx pbjs -t json -o game.json common/common-api/src/main/proto/game.proto
```

## 前端集成示例

### TypeScript/JavaScript

```typescript
// 导入生成的协议
import { game } from './proto/game';

// 创建请求
const loginRequest = game.proto.C2S_AccountLogin.create({
  loginType: game.proto.LoginType.LOGIN_TYPE_GUEST,
  deviceId: 'device-123',
  platform: 'pc',
  clientVersion: '1.0.0',
  channel: 'default'
});

// 序列化
const buffer = game.proto.C2S_AccountLogin.encode(loginRequest).finish();

// 发送到服务器
websocket.send(buffer);

// 接收响应
websocket.onmessage = (event) => {
  const response = game.proto.S2C_AccountLogin.decode(new Uint8Array(event.data));
  console.log('登录结果:', response.result.code);
};
```

### Unity C#

```bash
# 安装 protoc
# https://github.com/protocolbuffers/protobuf/releases

# 生成 C#
protoc --csharp_out=Assets/Scripts/Proto game.proto
```

```csharp
using Game.Proto;

// 创建请求
var request = new C2S_AccountLogin {
    LoginType = LoginType.LoginTypeGuest,
    DeviceId = SystemInfo.deviceUniqueIdentifier,
    Platform = "unity"
};

// 序列化
byte[] data = request.ToByteArray();

// 发送
websocket.Send(data);

// 接收
var response = S2C_AccountLogin.Parser.ParseFrom(receivedData);
```

## 协议号映射表

| 模块 | 协议号范围 | 说明 |
|------|-----------|------|
| 登录 | 0x0100 - 0x01FF | 握手、登录、选服、创角、进入游戏 |
| 玩家 | 0x0200 - 0x02FF | 玩家信息、背包、物品 |
| 背包 | 0x0300 - 0x03FF | 背包操作 |
| 装备 | 0x0400 - 0x04FF | 装备穿戴、强化 |
| 任务 | 0x0500 - 0x05FF | 任务系统 |
| 公会 | 0x0600 - 0x06FF | 公会相关 |
| 聊天 | 0x0700 - 0x07FF | 聊天系统 |
| 排行 | 0x0800 - 0x08FF | 排行榜 |
| 活动 | 0x0900 - 0x09FF | 活动系统 |
| 邮件 | 0x0A00 - 0x0AFF | 邮件系统 |
| 好友 | 0x0B00 - 0x0BFF | 好友系统 |
| 副本 | 0x0C00 - 0x0CFF | 副本系统 |
| 商城 | 0x0D00 - 0x0DFF | 商城系统 |
| 推送 | 0xF000 - 0xFFFF | 服务端推送消息 |

## 消息命名规范

| 方向 | 命名格式 | 示例 |
|------|----------|------|
| 客户端→服务端 | `C2S_功能名` | `C2S_AccountLogin` |
| 服务端→客户端 | `S2C_功能名` | `S2C_AccountLogin` |
| 服务端推送 | `S2C_推送名` | `S2C_KickOut` |

## 前端同步流程

```
1. 后端修改 game.proto
        ↓
2. 后端提交到 Git
        ↓
3. 前端拉取最新代码
        ↓
4. 前端执行生成脚本
        ↓
5. 前端更新协议处理代码
```

## 常见问题

### Q: 如何添加新协议?

1. 在 `game.proto` 中添加新的 message 定义
2. 按照命名规范: `C2S_Xxx` (请求) 和 `S2C_Xxx` (响应)
3. 执行 `mvn compile` 重新生成 Java 类
4. 在 Handler 中添加 `@Protocol` 注解的方法

### Q: TypeScript 类型不正确?

确保使用最新版本的 protobufjs:
```bash
npm install protobufjs@latest
```

### Q: 序列化后的数据太大?

1. 使用 `optional` 修饰可选字段，避免发送默认值
2. 考虑使用增量同步而非全量同步
3. 对于大型数据考虑压缩
