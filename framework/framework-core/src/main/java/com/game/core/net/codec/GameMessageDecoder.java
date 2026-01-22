package com.game.core.net.codec;

import com.game.common.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 游戏消息解码器
 * <p>
 * 协议格式:
 * | 4 bytes | 4 bytes | 2 bytes | 2 bytes | N bytes |
 * | 包长度  | 请求序号 | 协议号  | 方法号  | Protobuf 数据 |
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class GameMessageDecoder extends LengthFieldBasedFrameDecoder {

    public GameMessageDecoder() {
        super(
                ProtocolConstants.MAX_FRAME_LENGTH,  // 最大帧长度
                0,                                    // 长度字段偏移
                ProtocolConstants.LENGTH_FIELD_LENGTH,// 长度字段长度
                -ProtocolConstants.LENGTH_FIELD_LENGTH,// 长度调整值
                0                                     // 跳过字节数
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            return decodeFrame(frame);
        } finally {
            frame.release();
        }
    }

    /**
     * 解码帧数据
     */
    private GameMessage decodeFrame(ByteBuf frame) {
        // 读取包长度
        int length = frame.readInt();

        // 读取请求序号
        int seqId = frame.readInt();

        // 读取协议号
        int protocolId = frame.readUnsignedShort();

        // 读取方法号
        int methodId = frame.readUnsignedShort();

        // 读取 Protobuf 数据
        int bodyLength = length - ProtocolConstants.REQUEST_HEADER_LENGTH;
        byte[] body = new byte[bodyLength];
        if (bodyLength > 0) {
            frame.readBytes(body);
        }

        GameMessage message = new GameMessage();
        message.setSeqId(seqId);
        message.setProtocolId(protocolId);
        message.setMethodId(methodId);
        message.setBody(body);
        message.setType(GameMessage.Type.REQUEST);

        if (log.isDebugEnabled()) {
            log.debug("解码消息: seqId={}, protocolId={}, methodId={}, bodyLen={}", 
                    seqId, protocolId, methodId, bodyLength);
        }

        return message;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("消息解码异常, channel={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
