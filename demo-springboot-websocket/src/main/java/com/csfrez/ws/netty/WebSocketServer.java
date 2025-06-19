package com.csfrez.ws.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author
 * @date 2025/5/30 15:12
 * @email
 */
@Component
@Slf4j
public class WebSocketServer {

    //    @Autowired
//    private WebSocketServerInitializer webSocketServerInitializer;
//
//    @PostConstruct
//    public void start() {
//        new Thread(this::runNetty).start();
//    }
//
//    private void runNetty() {
//        EventLoopGroup bossGroup = new NioEventLoopGroup();
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap bootstrap = new ServerBootstrap();
//            bootstrap.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(webSocketServerInitializer)
//                    .option(ChannelOption.SO_BACKLOG, 128)
//                    .childOption(ChannelOption.SO_KEEPALIVE, true);
//            ChannelFuture future = bootstrap.bind(8888).sync();
//            log.info("Netty WebSocket Server started at port 8888.");
//            future.channel().closeFuture().sync();
//        } catch (Exception e) {
//            log.error("Error start Netty WebSocket Server", e);
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//    }
    @Value("${websocket.port:8888}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        new Thread(this::runNetty).start();
    }

    private void runNetty() {
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketInitializer());

            Channel channel = bootstrap.bind(port).sync().channel();
            log.info("WebSocket server started at port: {}", port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Error start WebSocket server", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket server stopped");
    }

}