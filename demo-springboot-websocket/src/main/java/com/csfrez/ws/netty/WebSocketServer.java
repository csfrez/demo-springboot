package com.csfrez.ws.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author
 * @date 2025/5/30 15:12
 * @email
 */
@Component
@Slf4j
public class WebSocketServer {

    @Autowired
    private WebSocketServerInitializer webSocketServerInitializer;

    @PostConstruct
    public void start() {
        new Thread(this::runNetty).start();
    }

    private void runNetty() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(webSocketServerInitializer);
            ChannelFuture future = bootstrap.bind(8888).sync();
            log.info("Netty WebSocket Server started at port 8888.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Error start Netty WebSocket Server", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}