package com.csfrez.ws.netty;

import com.csfrez.ws.handler.GlobalExceptionHandler;
import com.csfrez.ws.handler.WebSocketFrameHandler;
import com.csfrez.ws.handler.WebSocketHandshakeHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/5/30 15:10
 * @email
 */
@Component
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private WebSocketHandshakeHandler handshakeHandler;

    @Autowired
    private WebSocketFrameHandler frameHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(handshakeHandler);
        pipeline.addLast(new WebSocketServerProtocolHandler("/socket", null, true, 65536));
//        pipeline.addLast(new IdleStateHandler(30, 30, 60));
        pipeline.addLast(frameHandler);
        pipeline.addLast(new GlobalExceptionHandler());
    }
}