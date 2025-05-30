package com.csfrez.ws.netty;

import com.csfrez.ws.config.WebSocketConfig;
import com.csfrez.ws.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * @date 2025/5/30 10:15
 * @email
 */
//@Component
@Slf4j
public class NettyWebSocketServer {

    @Value("${websocket.token.heartbeat-interval}")
    private int heartbeatInterval;

    @Value("${websocket.token.timeout}")
    private int timeout;

//    @Autowired
//    private WebSocketConfig webSocketConfig;
//
//    @Autowired
//    private WebSocketHandler webSocketHandler;
//
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;

    private final WebSocketConfig webSocketConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public NettyWebSocketServer(WebSocketConfig webSocketConfig,
                                RedisTemplate<String, String> redisTemplate,
                                RabbitTemplate rabbitTemplate) {
        this.webSocketConfig = webSocketConfig;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

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
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(65536),
//                                    new HandshakeHandler(redisTemplate, webSocketConfig.getPath()),
                                    new WebSocketServerProtocolHandler(webSocketConfig.getPath(), null, true, 65536),
                                    new IdleStateHandler(heartbeatInterval, heartbeatInterval, timeout, TimeUnit.SECONDS),
                                    new WebSocketHandler(redisTemplate, rabbitTemplate)
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(8888).sync();
            log.info("Netty WebSocket Server started at port 8888.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Error start Netty WebSocket Server", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}