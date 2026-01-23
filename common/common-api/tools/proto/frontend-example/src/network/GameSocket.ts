/**
 * 游戏网络连接示例
 * 
 * 使用方法:
 * 1. npm install
 * 2. npm run generate
 * 3. 在项目中使用 GameSocket
 */

// @ts-ignore (生成后可用)
import * as proto from '../proto/game';

// 协议号定义
const ProtocolId = {
  // 登录模块 (0x01xx)
  C2S_HANDSHAKE: 0x0101,
  C2S_ACCOUNT_LOGIN: 0x0102,
  C2S_BIND_ACCOUNT: 0x0103,
  C2S_GET_SERVER_LIST: 0x0104,
  C2S_SELECT_SERVER: 0x0105,
  C2S_CHECK_ROLE_NAME: 0x0106,
  C2S_CREATE_ROLE: 0x0107,
  C2S_ENTER_GAME: 0x0108,
  C2S_LOGOUT: 0x0109,
  C2S_RECONNECT: 0x010A,
  C2S_HEARTBEAT: 0x010B,

  // 玩家模块 (0x02xx)
  C2S_GET_PLAYER_INFO: 0x0201,
  C2S_UPDATE_PLAYER: 0x0202,
  C2S_CHANGE_NAME: 0x0203,
  C2S_GET_BAG: 0x0210,
  C2S_USE_ITEM: 0x0211,
  C2S_SELL_ITEM: 0x0212,

  // 公会模块 (0x06xx)
  C2S_CREATE_GUILD: 0x0601,
  C2S_GET_GUILD_INFO: 0x0602,
  C2S_SEARCH_GUILD: 0x0603,
  C2S_APPLY_JOIN_GUILD: 0x0604,

  // 聊天模块 (0x07xx)
  C2S_SEND_CHAT: 0x0701,
  C2S_GET_CHAT_HISTORY: 0x0702,

  // 排行模块 (0x08xx)
  C2S_GET_RANK_LIST: 0x0801,
  C2S_GET_MY_RANK: 0x0802,

  // 推送消息 (0xFxxx)
  S2C_KICK_OUT: 0xF001,
  S2C_PLAYER_ATTR_CHANGE: 0xF002,
  S2C_LEVEL_UP: 0xF003,
  S2C_ITEM_CHANGE: 0xF004,
  S2C_CHAT_PUSH: 0xF010,
  S2C_SYSTEM_NOTICE: 0xF011,
};

// 消息头格式: [4字节长度][2字节协议号][N字节数据]
const HEADER_SIZE = 6;

type MessageHandler = (data: any) => void;

class GameSocket {
  private ws: WebSocket | null = null;
  private handlers: Map<number, MessageHandler> = new Map();
  private token: string = '';
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private heartbeatTimer: number | null = null;

  constructor(private url: string) {}

  // ==================== 连接管理 ====================

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);
      this.ws.binaryType = 'arraybuffer';

      this.ws.onopen = () => {
        console.log('[GameSocket] 连接成功');
        this.reconnectAttempts = 0;
        this.startHeartbeat();
        resolve();
      };

      this.ws.onclose = () => {
        console.log('[GameSocket] 连接关闭');
        this.stopHeartbeat();
        this.tryReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('[GameSocket] 连接错误', error);
        reject(error);
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };
    });
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.stopHeartbeat();
  }

  private tryReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[GameSocket] 重连次数超过上限');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    console.log(`[GameSocket] ${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连`);

    setTimeout(() => {
      this.connect().catch(() => {});
    }, delay);
  }

  // ==================== 心跳 ====================

  private startHeartbeat(): void {
    this.heartbeatTimer = window.setInterval(() => {
      this.sendHeartbeat();
    }, 30000);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private sendHeartbeat(): void {
    const request = proto.game.proto.C2S_Heartbeat.create({
      clientTime: Date.now()
    });
    this.send(ProtocolId.C2S_HEARTBEAT, request);
  }

  // ==================== 消息处理 ====================

  private handleMessage(data: ArrayBuffer): void {
    const view = new DataView(data);
    const length = view.getInt32(0, false);
    const protocolId = view.getInt16(4, false);
    const body = new Uint8Array(data, HEADER_SIZE);

    console.log(`[GameSocket] 收到消息: protocolId=0x${protocolId.toString(16)}, length=${length}`);

    const handler = this.handlers.get(protocolId);
    if (handler) {
      const decoded = this.decodeMessage(protocolId, body);
      handler(decoded);
    } else {
      console.warn(`[GameSocket] 未注册的协议处理器: 0x${protocolId.toString(16)}`);
    }
  }

  private decodeMessage(protocolId: number, body: Uint8Array): any {
    // 根据协议号解码消息
    switch (protocolId) {
      case ProtocolId.C2S_HANDSHAKE:
        return proto.game.proto.S2C_Handshake.decode(body);
      case ProtocolId.C2S_ACCOUNT_LOGIN:
        return proto.game.proto.S2C_AccountLogin.decode(body);
      case ProtocolId.C2S_GET_SERVER_LIST:
        return proto.game.proto.S2C_GetServerList.decode(body);
      case ProtocolId.C2S_SELECT_SERVER:
        return proto.game.proto.S2C_SelectServer.decode(body);
      case ProtocolId.C2S_CREATE_ROLE:
        return proto.game.proto.S2C_CreateRole.decode(body);
      case ProtocolId.C2S_ENTER_GAME:
        return proto.game.proto.S2C_EnterGame.decode(body);
      case ProtocolId.C2S_HEARTBEAT:
        return proto.game.proto.S2C_Heartbeat.decode(body);
      case ProtocolId.S2C_KICK_OUT:
        return proto.game.proto.S2C_KickOut.decode(body);
      case ProtocolId.S2C_CHAT_PUSH:
        return proto.game.proto.S2C_ChatPush.decode(body);
      default:
        console.warn(`[GameSocket] 未知消息类型: 0x${protocolId.toString(16)}`);
        return body;
    }
  }

  // ==================== 发送消息 ====================

  send(protocolId: number, message: any): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('[GameSocket] 连接未建立');
      return;
    }

    // 编码消息体
    const body = this.encodeMessage(protocolId, message);

    // 构建完整消息 [4字节长度][2字节协议号][N字节数据]
    const totalLength = HEADER_SIZE + body.length;
    const buffer = new ArrayBuffer(totalLength);
    const view = new DataView(buffer);

    view.setInt32(0, totalLength, false);  // 长度
    view.setInt16(4, protocolId, false);   // 协议号

    // 复制消息体
    const bodyView = new Uint8Array(buffer, HEADER_SIZE);
    bodyView.set(body);

    this.ws.send(buffer);
    console.log(`[GameSocket] 发送消息: protocolId=0x${protocolId.toString(16)}, length=${totalLength}`);
  }

  private encodeMessage(protocolId: number, message: any): Uint8Array {
    // 根据协议号编码消息
    switch (protocolId) {
      case ProtocolId.C2S_HANDSHAKE:
        return proto.game.proto.C2S_Handshake.encode(message).finish();
      case ProtocolId.C2S_ACCOUNT_LOGIN:
        return proto.game.proto.C2S_AccountLogin.encode(message).finish();
      case ProtocolId.C2S_GET_SERVER_LIST:
        return proto.game.proto.C2S_GetServerList.encode(message).finish();
      case ProtocolId.C2S_SELECT_SERVER:
        return proto.game.proto.C2S_SelectServer.encode(message).finish();
      case ProtocolId.C2S_CREATE_ROLE:
        return proto.game.proto.C2S_CreateRole.encode(message).finish();
      case ProtocolId.C2S_ENTER_GAME:
        return proto.game.proto.C2S_EnterGame.encode(message).finish();
      case ProtocolId.C2S_HEARTBEAT:
        return proto.game.proto.C2S_Heartbeat.encode(message).finish();
      default:
        console.warn(`[GameSocket] 未知消息类型: 0x${protocolId.toString(16)}`);
        return new Uint8Array(0);
    }
  }

  // ==================== 注册处理器 ====================

  on(protocolId: number, handler: MessageHandler): void {
    this.handlers.set(protocolId, handler);
  }

  off(protocolId: number): void {
    this.handlers.delete(protocolId);
  }

  // ==================== 业务接口 ====================

  async handshake(): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_HANDSHAKE, (data) => {
        resolve(data);
      });

      const request = proto.game.proto.C2S_Handshake.create({
        clientVersion: '1.0.0',
        platform: 'web',
        deviceId: this.getDeviceId(),
        language: 'zh-CN',
        channel: 'default'
      });

      this.send(ProtocolId.C2S_HANDSHAKE, request);
    });
  }

  async guestLogin(): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_ACCOUNT_LOGIN, (data) => {
        if (data.token) {
          this.token = data.token;
        }
        resolve(data);
      });

      const request = proto.game.proto.C2S_AccountLogin.create({
        loginType: proto.game.proto.LoginType.LOGIN_TYPE_GUEST,
        deviceId: this.getDeviceId(),
        platform: 'web',
        clientVersion: '1.0.0',
        channel: 'default'
      });

      this.send(ProtocolId.C2S_ACCOUNT_LOGIN, request);
    });
  }

  async getServerList(): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_GET_SERVER_LIST, (data) => {
        resolve(data);
      });

      const request = proto.game.proto.C2S_GetServerList.create({});
      this.send(ProtocolId.C2S_GET_SERVER_LIST, request);
    });
  }

  async selectServer(serverId: number): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_SELECT_SERVER, (data) => {
        resolve(data);
      });

      const request = proto.game.proto.C2S_SelectServer.create({
        serverId
      });

      this.send(ProtocolId.C2S_SELECT_SERVER, request);
    });
  }

  async createRole(roleName: string, avatarId: number, gender: number, profession: number): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_CREATE_ROLE, (data) => {
        resolve(data);
      });

      const request = proto.game.proto.C2S_CreateRole.create({
        roleName,
        avatarId,
        gender,
        profession
      });

      this.send(ProtocolId.C2S_CREATE_ROLE, request);
    });
  }

  async enterGame(roleId: number): Promise<any> {
    return new Promise((resolve) => {
      this.on(ProtocolId.C2S_ENTER_GAME, (data) => {
        resolve(data);
      });

      const request = proto.game.proto.C2S_EnterGame.create({
        roleId
      });

      this.send(ProtocolId.C2S_ENTER_GAME, request);
    });
  }

  // ==================== 工具方法 ====================

  private getDeviceId(): string {
    let deviceId = localStorage.getItem('device_id');
    if (!deviceId) {
      deviceId = 'web_' + Math.random().toString(36).substring(2);
      localStorage.setItem('device_id', deviceId);
    }
    return deviceId;
  }
}

export { GameSocket, ProtocolId };
