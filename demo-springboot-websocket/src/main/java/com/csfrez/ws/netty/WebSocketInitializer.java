package com.csfrez.ws.netty;

import com.csfrez.ws.handler.ObjectHandshakeHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author
 * @date 2025/6/3 11:46
 * @email
 */
public class WebSocketInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
//        ch.pipeline()
//                // HTTP编解码器
//                .addLast(new HttpServerCodec())
//                // 聚合HTTP请求
//                .addLast(new HttpObjectAggregator(65536))
//                // 心跳检测 (30秒未读触发)
//                .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
//                // WebSocket协议处理器
//                .addLast(new WebSocketServerProtocolHandler("/socket", null, true))
//                // 自定义业务处理器
//                .addLast(new ObjectWebSocketHandler());
        ChannelPipeline pipeline = ch.pipeline();

//        // 添加HTTP编解码器（仅用于握手阶段）
//        pipeline.addLast(new HttpServerCodec());
//        pipeline.addLast(new HttpObjectAggregator(65536));
//
//        // 添加自定义握手处理器
//        pipeline.addLast(new ObjectHandshakeHandler());
//
//        // 心跳检测
//        pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));

        // 添加HTTP编解码器（指定处理器名称）
        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));

        // 添加自定义握手处理器（指定名称）
        pipeline.addLast("handshake-handler", new ObjectHandshakeHandler());

        // 心跳检测（指定名称）
        pipeline.addLast("idle-handler", new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
    }
}