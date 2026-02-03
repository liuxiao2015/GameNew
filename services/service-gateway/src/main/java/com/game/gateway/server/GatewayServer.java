package com.game.gateway.server;

import com.game.core.net.codec.GameMessageDecoder;
import com.game.core.net.codec.GameMessageEncoder;
import com.game.gateway.handler.GatewayChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 网关服务器
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayServer {

    @Value("${gateway.server.port:8888}")
    private int port;

    @Value("${gateway.server.boss-threads:1}")
    private int bossThreads;

    @Value("${gateway.server.worker-threads:0}")
    private int workerThreads;

    @Value("${gateway.server.reader-idle:60}")
    private int readerIdle;

    @Value("${gateway.server.writer-idle:30}")
    private int writerIdle;

    private final GatewayChannelHandler gatewayChannelHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 心跳检测
                        pipeline.addLast("idleStateHandler",
                            new IdleStateHandler(readerIdle, writerIdle, 0, TimeUnit.SECONDS));
                        
                        // 编解码器
                        pipeline.addLast("decoder", new GameMessageDecoder());
                        pipeline.addLast("encoder", new GameMessageEncoder());
                        
                        // 业务处理器
                        pipeline.addLast("handler", gatewayChannelHandler);
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("网关服务器启动成功, 端口: {}", port);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("网关服务器启动失败", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("正在关闭网关服务器...");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("网关服务器已关闭");
    }
}
