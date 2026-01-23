# 协议常量参考文档

> **版本**: v1.0  
> **更新日期**: 2026-01  
> **说明**: 本文档定义所有协议号常量，用于 @Protocol 注解

---

## 协议号结构

```
完整协议号 = 模块号 (高 8 位) + 方法号 (低 8 位)

示例: 
  模块号 0x01 (登录) + 方法号 0x02 (账号登录) = 0x0102
```

---

## 模块号定义

位置: `common/common-api/src/main/java/com/game/api/common/ProtocolConstants.java`

| 模块 | 模块号 | 协议号范围 | 说明 |
|------|--------|-----------|------|
| Login | 0x01 (0x0100) | 0x0100-0x01FF | 登录相关 |
| Player | 0x02 (0x0200) | 0x0200-0x02FF | 玩家相关 |
| Bag | 0x03 (0x0300) | 0x0300-0x03FF | 背包相关 |
| Equipment | 0x04 (0x0400) | 0x0400-0x04FF | 装备相关 |
| Quest | 0x05 (0x0500) | 0x0500-0x05FF | 任务相关 |
| Guild | 0x06 (0x0600) | 0x0600-0x06FF | 公会相关 |
| Chat | 0x07 (0x0700) | 0x0700-0x07FF | 聊天相关 |
| Rank | 0x08 (0x0800) | 0x0800-0x08FF | 排行相关 |
| Activity | 0x09 (0x0900) | 0x0900-0x09FF | 活动相关 |
| Mail | 0x0A (0x0A00) | 0x0A00-0x0AFF | 邮件相关 |
| Friend | 0x0B (0x0B00) | 0x0B00-0x0BFF | 好友相关 |
| Dungeon | 0x0C (0x0C00) | 0x0C00-0x0CFF | 副本相关 |
| Shop | 0x0D (0x0D00) | 0x0D00-0x0DFF | 商城相关 |

---

## 方法号定义

位置: `common/common-api/src/main/java/com/game/api/common/MethodId.java`

### Login 模块 (0x01)

| 方法 | 方法号 | 完整协议号 | 处理位置 | 说明 |
|------|--------|-----------|----------|------|
| HANDSHAKE | 0x01 | 0x0101 | **Gateway** | 握手 |
| ACCOUNT_LOGIN | 0x02 | 0x0102 | Login Service | 账号登录 |
| BIND_ACCOUNT | 0x03 | 0x0103 | Login Service | 绑定账号 |
| GET_SERVER_LIST | 0x04 | 0x0104 | Login Service | 获取服务器列表 |
| SELECT_SERVER | 0x05 | 0x0105 | Login Service | 选择服务器 |
| CHECK_ROLE_NAME | 0x06 | 0x0106 | Login Service | 检查角色名 |
| CREATE_ROLE | 0x07 | 0x0107 | Login Service | 创建角色 |
| ENTER_GAME | 0x08 | 0x0108 | Login Service | 进入游戏 |
| LOGOUT | 0x09 | 0x0109 | Login Service | 登出 |
| RECONNECT | 0x0A | 0x010A | Login Service | 重连 |
| HEARTBEAT | 0x0B | 0x010B | **Gateway** | 心跳 |

### Player 模块 (0x02)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_INFO | 0x01 | 0x0201 | 获取玩家信息 |
| UPDATE_INFO | 0x02 | 0x0202 | 更新玩家信息 |
| CHANGE_NAME | 0x03 | 0x0203 | 修改名字 |
| GET_BAG | 0x10 | 0x0210 | 获取背包 |
| USE_ITEM | 0x11 | 0x0211 | 使用物品 |
| SELL_ITEM | 0x12 | 0x0212 | 出售物品 |
| ARRANGE_BAG | 0x13 | 0x0213 | 整理背包 |

### Bag 模块 (0x03)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0301 | 获取背包列表 |
| USE_ITEM | 0x02 | 0x0302 | 使用物品 |
| SELL_ITEM | 0x03 | 0x0303 | 出售物品 |
| DROP_ITEM | 0x04 | 0x0304 | 丢弃物品 |
| ARRANGE | 0x05 | 0x0305 | 整理背包 |
| DECOMPOSE | 0x06 | 0x0306 | 分解物品 |

### Equipment 模块 (0x04)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| EQUIP | 0x01 | 0x0401 | 穿戴装备 |
| UNEQUIP | 0x02 | 0x0402 | 卸下装备 |
| ENHANCE | 0x03 | 0x0403 | 强化装备 |
| STAR_UP | 0x04 | 0x0404 | 升星 |
| INLAY_GEM | 0x05 | 0x0405 | 镶嵌宝石 |
| REMOVE_GEM | 0x06 | 0x0406 | 拆卸宝石 |

### Quest 模块 (0x05)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0501 | 获取任务列表 |
| ACCEPT | 0x02 | 0x0502 | 接取任务 |
| SUBMIT | 0x03 | 0x0503 | 提交任务 |
| ABANDON | 0x04 | 0x0504 | 放弃任务 |
| CLAIM_REWARD | 0x05 | 0x0505 | 领取奖励 |

### Guild 模块 (0x06)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| CREATE | 0x01 | 0x0601 | 创建公会 |
| GET_INFO | 0x02 | 0x0602 | 获取公会信息 |
| SEARCH | 0x03 | 0x0603 | 搜索公会 |
| APPLY_JOIN | 0x04 | 0x0604 | 申请加入公会 |
| HANDLE_APPLY | 0x05 | 0x0605 | 处理加入申请 |
| LEAVE | 0x06 | 0x0606 | 退出公会 |
| KICK_MEMBER | 0x07 | 0x0607 | 踢出成员 |
| DONATE | 0x08 | 0x0608 | 公会捐献 |
| CHANGE_POSITION | 0x09 | 0x0609 | 修改成员职位 |
| TRANSFER_LEADER | 0x0A | 0x060A | 转让会长 |
| CHANGE_SETTING | 0x0B | 0x060B | 修改公会设置 |

### Chat 模块 (0x07)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| SEND | 0x01 | 0x0701 | 发送聊天消息 |
| GET_HISTORY | 0x02 | 0x0702 | 获取聊天记录 |

### Rank 模块 (0x08)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0801 | 获取排行榜 |
| GET_MY_RANK | 0x02 | 0x0802 | 获取自己排名 |

### Activity 模块 (0x09)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0901 | 获取活动列表 |
| GET_DETAIL | 0x02 | 0x0902 | 获取活动详情 |
| CLAIM_REWARD | 0x03 | 0x0903 | 领取活动奖励 |

### Mail 模块 (0x0A)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0A01 | 获取邮件列表 |
| READ | 0x02 | 0x0A02 | 读取邮件 |
| CLAIM_ATTACHMENT | 0x03 | 0x0A03 | 领取附件 |
| DELETE | 0x04 | 0x0A04 | 删除邮件 |
| CLAIM_ALL | 0x05 | 0x0A05 | 一键领取 |

### Friend 模块 (0x0B)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0B01 | 获取好友列表 |
| ADD | 0x02 | 0x0B02 | 添加好友 |
| DELETE | 0x03 | 0x0B03 | 删除好友 |
| HANDLE_REQUEST | 0x04 | 0x0B04 | 处理好友请求 |
| SEARCH_PLAYER | 0x05 | 0x0B05 | 搜索玩家 |

### Dungeon 模块 (0x0C)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0C01 | 获取副本列表 |
| ENTER | 0x02 | 0x0C02 | 进入副本 |
| EXIT | 0x03 | 0x0C03 | 退出副本 |
| SWEEP | 0x04 | 0x0C04 | 扫荡副本 |
| CLAIM_REWARD | 0x05 | 0x0C05 | 领取奖励 |

### Shop 模块 (0x0D)

| 方法 | 方法号 | 完整协议号 | 说明 |
|------|--------|-----------|------|
| GET_LIST | 0x01 | 0x0D01 | 获取商城列表 |
| BUY | 0x02 | 0x0D02 | 购买商品 |
| REFRESH | 0x03 | 0x0D03 | 刷新商城 |

---

## 使用示例

### 定义协议处理器

```java
import com.game.api.common.MethodId;
import com.game.api.common.ProtocolConstants;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;

@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_PLAYER, value = "玩家模块")
public class PlayerHandler extends BaseHandler {
    
    // 使用 MethodId 常量，避免硬编码
    @Protocol(methodId = MethodId.Player.GET_INFO, desc = "获取玩家信息")
    public Message getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
        // ...
    }
    
    @Protocol(methodId = MethodId.Player.CHANGE_NAME, desc = "修改名字")
    public Message changeName(Session session, C2S_ChangeName request) {
        // ...
    }
}
```

### 添加新模块

1. 在 `ProtocolConstants.java` 中添加模块号：

```java
public static final int PROTOCOL_NEW_MODULE = 0x0E00;
```

2. 在 `MethodId.java` 中添加方法号：

```java
public static final class NewModule {
    private NewModule() {}
    
    public static final int ACTION_1 = 0x01;
    public static final int ACTION_2 = 0x02;
}
```

3. 创建协议处理器：

```java
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_NEW_MODULE, value = "新模块")
public class NewModuleHandler extends BaseHandler {
    
    @Protocol(methodId = MethodId.NewModule.ACTION_1, desc = "操作1")
    public Message action1(Session session, C2S_Action1 request) {
        // ...
    }
}
```

---

## 协议处理位置说明

| 位置 | 协议类型 | 说明 |
|------|---------|------|
| Gateway | 握手 (0x0101) | 连接层协议，由网关独占处理 |
| Gateway | 心跳 (0x010B) | 连接层协议，由网关独占处理 |
| Login Service | 登录相关 | 账号登录、选服、创角、进入游戏 |
| Game Service | 玩家相关 | 玩家数据、背包、装备等 |
| Guild Service | 公会相关 | 公会数据、成员操作 |
| Chat Service | 聊天相关 | 聊天消息 |
| Rank Service | 排行相关 | 排行榜查询 |

---

## 注意事项

1. **避免协议号冲突**: 每个模块的方法号必须唯一
2. **使用常量**: 始终使用 `MethodId` 常量，不要硬编码
3. **网关协议**: 握手和心跳由 Gateway 处理，不要在业务服务中重复注册
4. **协议文档**: Proto 文件位于 `common/common-api/src/main/proto/game.proto`

---

**文档版本**: v1.0  
**最后更新**: 2026-01
