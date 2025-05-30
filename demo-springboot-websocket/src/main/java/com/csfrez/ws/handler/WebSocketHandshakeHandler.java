package com.csfrez.ws.handler;

import com.csfrez.ws.servie.TokenService;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/5/30 15:07
 * @email
 */
@Component
@ChannelHandler.Sharable
@Slf4j
public class WebSocketHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Autowired
    private TokenService tokenService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        String path = decoder.path();

        String[] parts = path.split("/");
        if (parts.length < 3 || !"socket".equals(parts[1])) {
            sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
            ctx.close();
            return;
        }

        String token = parts[2];
        if (!tokenService.validateToken(token)) {
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            ctx.close();
            return;
        }

        // 合法请求，继续后续处理（包括 WebSocket 握手）
        ctx.fireChannelRead(request.retain());
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        HttpUtil.setContentLength(response, 0);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ChannelFuture f = ctx.writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(req) || status.code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

//    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
//        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
//        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocketHandshakeHandler.exceptionCaught", cause);
        cause.printStackTrace();
        ctx.close();
    }
}