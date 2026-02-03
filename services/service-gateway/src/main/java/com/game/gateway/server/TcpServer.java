package com.game.gateway.server;

import com.game.core.net.codec.GameMessageDecoder;
import com.game.core.net.codec.GameMessageEncoder;
import com.game.core.net.handler.GameChannelHandler;
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
 * TCP 网关服务器
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcpServer {

    private final GameChannelHandler gameChannelHandler;

    @Value("${gateway.tcp.port:9000}")
    private int port;

    @Value("${gateway.tcp.boss-threads:1}")
    private int bossThreads;

    @Value("${gateway.tcp.worker-threads:0}")
    private int workerThreads;

    @Value("${gateway.tcp.backlog:1024}")
    private int backlog;

    @Value("${gateway.tcp.heartbeat.interval-seconds:30}")
    private int heartbeatInterval;

    @Value("${gateway.tcp.heartbeat.timeout-seconds:90}")
    private int heartbeatTimeout;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        new Thread(this::startServer, "tcp-server-starter").start();
    }

    private void startServer() {
        // 计算 worker 线程数
        int workers = workerThreads > 0 ? workerThreads : Runtime.getRuntime().availableProcessors() * 2;

        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workers);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, backlog)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_RCVBUF, 65535)
                    .childOption(ChannelOption.SO_SNDBUF, 65535)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 空闲检测 (读空闲超时)
                            pipeline.addLast("idle", new IdleStateHandler(
                                    heartbeatTimeout, 0, 0, TimeUnit.SECONDS));

                            // 编解码器
                            pipeline.addLast("decoder", new GameMessageDecoder());
                            pipeline.addLast("encoder", new GameMessageEncoder());

                            // 业务处理器
                            pipeline.addLast("handler", gameChannelHandler);
                        }
                    });

            // 绑定端口
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("TCP 网关服务器启动成功: port={}, bossThreads={}, workerThreads={}", 
                    port, bossThreads, workers);

            // 等待服务器关闭
            serverChannel.closeFuture().sync();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("TCP 服务器启动被中断", e);
        } catch (Exception e) {
            log.error("TCP 服务器启动失败", e);
        } finally {
            shutdown();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("TCP 网关服务器开始关闭...");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }

        log.info("TCP 网关服务器已关闭");
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 判断服务器是否运行中
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}
