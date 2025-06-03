package com.csfrez.ws.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

import java.util.List;
import java.util.Map;

public class TokenAuthHandler extends ChannelInboundHandlerAdapter {

    private static final AsciiString UNAUTHORIZED = AsciiString.cached("Unauthorized");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            if (!validateToken(request) || !isCorrectPath(request)) {
                sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
                request.release();
                return;
            }
            ctx.fireChannelRead(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean isCorrectPath(FullHttpRequest request) {
        return "/socket".equals(new QueryStringDecoder(request.uri()).path());
    }

    private boolean validateToken(FullHttpRequest request) {
        Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
        List<String> tokens = params.get("token");
        return tokens != null && tokens.contains("123456");
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
//        FullHttpResponse response = new DefaultFullHttpResponse(
//                HttpVersion.HTTP_1_1, status,
//                Unpooled.copiedBuffer(UNAUTHORIZED)
//        );
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}