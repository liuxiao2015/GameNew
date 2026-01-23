package com.game.robot.client;

import com.game.core.net.codec.GameMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游戏客户端
 * <p>
 * 模拟客户端与服务器通信
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class GameClient {

    private final String host;
    private final int port;
    private final EventLoopGroup group;
    private Channel channel;

    @Getter
    private volatile boolean connected = false;

    /**
     * 请求序号生成器
     */
    private final AtomicInteger seqIdGenerator = new AtomicInteger(0);

    /**
     * 等待响应的请求
     */
    private final Map<Integer, CompletableFuture<GameMessage>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 消息监听器
     */
    private MessageListener messageListener;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.group = new NioEventLoopGroup(1);
    }

    /**
     * 连接服务器
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 长度编解码
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        // 心跳
                        pipeline.addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));

                        // 消息处理
                        pipeline.addLast(new ClientMessageHandler());
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) cf -> {
            if (cf.isSuccess()) {
                channel = cf.channel();
                connected = true;
                log.info("连接服务器成功: {}:{}", host, port);
                future.complete(null);
            } else {
                log.error("连接服务器失败: {}:{}", host, port, cf.cause());
                future.completeExceptionally(cf.cause());
            }
        });

        return future;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        connected = false;
        log.info("断开服务器连接");
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        disconnect();
        group.shutdownGracefully();
    }

    /**
     * 发送请求并等待响应
     */
    public CompletableFuture<GameMessage> sendRequest(int protocolId, int methodId, byte[] body) {
        if (!connected || channel == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("未连接服务器"));
        }

        int seqId = seqIdGenerator.incrementAndGet();
        GameMessage request = GameMessage.createRequest(seqId, protocolId, methodId, body);

        CompletableFuture<GameMessage> future = new CompletableFuture<>();
        pendingRequests.put(seqId, future);

        // 设置超时
        future.orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((msg, ex) -> pendingRequests.remove(seqId));

        channel.writeAndFlush(request);
        return future;
    }

    /**
     * 发送消息 (不等待响应)
     */
    public void send(GameMessage message) {
        if (connected && channel != null) {
            channel.writeAndFlush(message);
        }
    }

    /**
     * 设置消息监听器
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * 处理收到的消息
     */
    private void handleMessage(GameMessage message) {
        if (message.getType() == GameMessage.Type.RESPONSE) {
            // 响应消息
            CompletableFuture<GameMessage> future = pendingRequests.remove(message.getSeqId());
            if (future != null) {
                future.complete(message);
            }
        } else if (message.getType() == GameMessage.Type.PUSH) {
            // 推送消息
            if (messageListener != null) {
                messageListener.onPush(message);
            }
        }
    }

    /**
     * 消息监听器
     */
    public interface MessageListener {
        void onPush(GameMessage message);
    }

    /**
     * 客户端消息处理器
     */
    private class ClientMessageHandler extends SimpleChannelInboundHandler<GameMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) {
            handleMessage(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            connected = false;
            log.warn("与服务器断开连接");

            // 完成所有等待的请求
            pendingRequests.forEach((seqId, future) ->
                    future.completeExceptionally(new RuntimeException("连接断开")));
            pendingRequests.clear();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("客户端异常", cause);
            ctx.close();
        }
    }
}
