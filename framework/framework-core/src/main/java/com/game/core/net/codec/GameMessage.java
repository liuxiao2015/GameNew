package com.game.core.net.codec;

import com.google.protobuf.Message;
import lombok.Data;

/**
 * 游戏消息对象
 *
 * @author GameServer
 */
@Data
public class GameMessage {

    /**
     * 消息类型
     */
    public enum Type {
        /**
         * 请求 (客户端 -> 服务端)
         */
        REQUEST,
        /**
         * 响应 (服务端 -> 客户端)
         */
        RESPONSE,
        /**
         * 推送 (服务端主动推送)
         */
        PUSH
    }

    /**
     * 消息类型
     */
    private Type type;

    /**
     * 请求序号 (用于请求-响应配对)
     */
    private int seqId;

    /**
     * 协议号
     */
    private int protocolId;

    /**
     * 方法号
     */
    private int methodId;

    /**
     * 错误码 (响应消息使用)
     */
    private int errorCode;

    /**
     * 推送类型 (推送消息使用)
     */
    private int pushType;

    /**
     * 消息体 (原始字节数据)
     */
    private byte[] body;

    /**
     * 解析后的 Protobuf 消息对象 (延迟解析)
     */
    private transient Message protoMessage;

    /**
     * 创建请求消息
     */
    public static GameMessage createRequest(int seqId, int protocolId, int methodId, byte[] body) {
        GameMessage msg = new GameMessage();
        msg.setType(Type.REQUEST);
        msg.setSeqId(seqId);
        msg.setProtocolId(protocolId);
        msg.setMethodId(methodId);
        msg.setBody(body);
        return msg;
    }

    /**
     * 创建响应消息 (字节数组)
     */
    public static GameMessage createResponse(int seqId, int errorCode, byte[] body) {
        GameMessage msg = new GameMessage();
        msg.setType(Type.RESPONSE);
        msg.setSeqId(seqId);
        msg.setErrorCode(errorCode);
        msg.setBody(body);
        return msg;
    }

    /**
     * 创建响应消息 (带协议号)
     */
    public static GameMessage createResponse(int seqId, int protocolId, int methodId, 
                                             int errorCode, Message protoMessage) {
        GameMessage msg = new GameMessage();
        msg.setType(Type.RESPONSE);
        msg.setSeqId(seqId);
        msg.setProtocolId(protocolId);
        msg.setMethodId(methodId);
        msg.setErrorCode(errorCode);
        msg.setProtoMessage(protoMessage);
        if (protoMessage != null) {
            msg.setBody(protoMessage.toByteArray());
        }
        return msg;
    }

    /**
     * 创建推送消息
     */
    public static GameMessage createPush(int pushType, int protocolId, byte[] body) {
        GameMessage msg = new GameMessage();
        msg.setType(Type.PUSH);
        msg.setPushType(pushType);
        msg.setProtocolId(protocolId);
        msg.setBody(body);
        return msg;
    }

    /**
     * 创建推送消息 (Protobuf)
     */
    public static GameMessage createPush(int pushType, int protocolId, Message protoMessage) {
        GameMessage msg = new GameMessage();
        msg.setType(Type.PUSH);
        msg.setPushType(pushType);
        msg.setProtocolId(protocolId);
        msg.setProtoMessage(protoMessage);
        if (protoMessage != null) {
            msg.setBody(protoMessage.toByteArray());
        }
        return msg;
    }

    /**
     * 根据请求创建响应
     */
    public GameMessage toResponse(int errorCode, byte[] body) {
        return createResponse(this.seqId, errorCode, body);
    }

    /**
     * 根据请求创建响应 (Protobuf)
     */
    public GameMessage toResponse(int errorCode, Message protoMessage) {
        return createResponse(this.seqId, this.protocolId, this.methodId, errorCode, protoMessage);
    }

    /**
     * 获取协议标识 (协议号 + 方法号)
     */
    public int getProtocolKey() {
        return (protocolId << 16) | methodId;
    }

    /**
     * 设置协议标识
     */
    public void setProtocolKey(int protocolKey) {
        this.protocolId = (protocolKey >> 16) & 0xFFFF;
        this.methodId = protocolKey & 0xFFFF;
    }

    /**
     * 解析并获取 Protobuf 消息
     */
    @SuppressWarnings("unchecked")
    public <T extends Message> T parseBody(com.google.protobuf.Parser<T> parser) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            T message = parser.parseFrom(body);
            this.protoMessage = message;
            return message;
        } catch (Exception e) {
            throw new RuntimeException("解析 Protobuf 消息失败", e);
        }
    }

    // ==================== 兼容方法 (网关使用) ====================

    /**
     * 获取命令号 (兼容方法)
     */
    public int getCmd() {
        return getProtocolKey();
    }

    /**
     * 设置命令号 (兼容方法)
     */
    public void setCmd(int cmd) {
        this.protocolId = cmd >> 16;
        this.methodId = cmd & 0xFFFF;
    }

    /**
     * 获取序号 (兼容方法)
     */
    public int getSeq() {
        return seqId;
    }

    /**
     * 设置序号 (兼容方法)
     */
    public void setSeq(int seq) {
        this.seqId = seq;
    }

    /**
     * 获取数据 (兼容方法)
     */
    public byte[] getData() {
        return body;
    }

    /**
     * 设置数据 (兼容方法)
     */
    public void setData(byte[] data) {
        this.body = data;
    }

    @Override
    public String toString() {
        return String.format("GameMessage[type=%s, protocolId=0x%04X, methodId=0x%04X, seqId=%d, errorCode=%d, bodyLen=%d]",
                type, protocolId, methodId, seqId, errorCode, body != null ? body.length : 0);
    }
}
