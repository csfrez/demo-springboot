package com.csfrez.ws.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author
 * @date 2025/6/3 12:46
 * @email
 */
@Slf4j
public class ObjectHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String VALID_TOKEN = "123456";
    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 1. 验证Token
        String token = getTokenFromUri(request.uri());
        if (!VALID_TOKEN.equals(token)) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED));
            return;
        }

        // 2. 执行WebSocket握手
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8888/socket", null, false);
        handshaker = wsFactory.newHandshaker(request);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 握手成功后重建pipeline
            handshaker.handshake(ctx.channel(), request).addListener(future -> {
                if (future.isSuccess()) {
                    rebuildPipelineAfterHandshake(ctx);
                }
            });
        }
    }

    private void rebuildPipelineAfterHandshake(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();

//        // 移除HTTP处理器和当前握手处理器
//        pipeline.remove(HttpServerCodec.class);
//        pipeline.remove(HttpObjectAggregator.class);
//        pipeline.remove(ObjectHandshakeHandler.class);
//
//        // 添加WebSocket处理器
//        pipeline.addLast(new ObjectWebSocketHandler());

        // 安全移除处理器 - 只移除存在的处理器
        removeHandlerByName(pipeline, "http-codec");
        removeHandlerByName(pipeline, "http-aggregator");
        removeHandlerByName(pipeline, "handshake-handler");

        // 添加WebSocket帧处理器
        pipeline.addLast("websocket-handler", new ObjectWebSocketHandler());

        System.out.println("WebSocket handshake complete. Pipeline rebuilt.");
    }

    // 安全移除处理器方法
    private void removeHandlerByName(ChannelPipeline pipeline, String handlerName) {
        log.info("Removing handler key: {}, value: {}", handlerName, pipeline.get(handlerName));
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }

    private String getTokenFromUri(String uri) {
        // 解析token参数
        UriDecoder decoder = new UriDecoder(uri);
        return decoder.getParameter("token");
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}