package com.csfrez.ws.netty;

import com.csfrez.ws.handler.TokenAuthHandler;
import com.csfrez.ws.handler.WebSocketFrameHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author
 * @date 2025/5/30 15:10
 * @email
 */
//@Component
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

//    @Autowired
//    private WebSocketHandshakeHandler handshakeHandler;
//
//    @Autowired
//    private WebSocketFrameHandler frameHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
//        pipeline.addLast(handshakeHandler);
        pipeline.addLast(new TokenAuthHandler());
        new WebSocketServerProtocolHandler("/socket", null, true);
//        pipeline.addLast(new WebSocketServerProtocolHandler("/socket", null, true, 65536, true));
//        pipeline.addLast(new WebSocketServerProtocolHandler("/socket"));
        pipeline.addLast(new IdleStateHandler(30, 10, 0, TimeUnit.SECONDS));
//        pipeline.addLast(frameHandler);
        pipeline.addLast(new WebSocketFrameHandler());
//        pipeline.addLast(new GlobalExceptionHandler());
    }
}