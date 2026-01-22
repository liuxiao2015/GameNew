package com.game.core.net.codec;

import com.game.common.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 游戏消息编码器
 * <p>
 * 响应协议格式:
 * | 4 bytes | 4 bytes | 4 bytes | N bytes |
 * | 包长度  | 请求序号 | 错误码  | Protobuf 数据 |
 * <p>
 * 推送协议格式:
 * | 4 bytes | 2 bytes | 2 bytes | N bytes |
 * | 包长度  | 推送类型 | 协议号  | Protobuf 数据 |
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class GameMessageEncoder extends MessageToByteEncoder<GameMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, GameMessage msg, ByteBuf out) {
        if (msg.getType() == GameMessage.Type.RESPONSE) {
            encodeResponse(msg, out);
        } else if (msg.getType() == GameMessage.Type.PUSH) {
            encodePush(msg, out);
        } else {
            log.warn("未知的消息类型: {}", msg.getType());
        }
    }

    /**
     * 编码响应消息
     */
    private void encodeResponse(GameMessage msg, ByteBuf out) {
        byte[] body = msg.getBody();
        int bodyLength = body != null ? body.length : 0;
        int totalLength = ProtocolConstants.RESPONSE_HEADER_LENGTH + bodyLength;

        // 写入包长度
        out.writeInt(totalLength);

        // 写入请求序号
        out.writeInt(msg.getSeqId());

        // 写入错误码
        out.writeInt(msg.getErrorCode());

        // 写入 Protobuf 数据
        if (bodyLength > 0) {
            out.writeBytes(body);
        }

        if (log.isDebugEnabled()) {
            log.debug("编码响应: seqId={}, errorCode={}, bodyLen={}", 
                    msg.getSeqId(), msg.getErrorCode(), bodyLength);
        }
    }

    /**
     * 编码推送消息
     */
    private void encodePush(GameMessage msg, ByteBuf out) {
        byte[] body = msg.getBody();
        int bodyLength = body != null ? body.length : 0;
        int totalLength = ProtocolConstants.PUSH_HEADER_LENGTH + bodyLength;

        // 写入包长度
        out.writeInt(totalLength);

        // 写入推送类型
        out.writeShort(msg.getPushType());

        // 写入协议号
        out.writeShort(msg.getProtocolId());

        // 写入 Protobuf 数据
        if (bodyLength > 0) {
            out.writeBytes(body);
        }

        if (log.isDebugEnabled()) {
            log.debug("编码推送: pushType={}, protocolId={}, bodyLen={}", 
                    msg.getPushType(), msg.getProtocolId(), bodyLength);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("消息编码异常, channel={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
