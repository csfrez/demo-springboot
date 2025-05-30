package com.csfrez.ws.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author
 * @date 2025/5/30 12:38
 * @email
 */
// HandshakeHandler.java
@ChannelHandler.Sharable
public class HandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final RedisTemplate<String, String> redisTemplate;
    private final String webSocketPath;

    public HandshakeHandler(RedisTemplate<String, String> redisTemplate, String webSocketPath) {
        this.redisTemplate = redisTemplate;
        this.webSocketPath = webSocketPath;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        if (!uri.startsWith(webSocketPath)) {
            // 请求路径不匹配
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            ctx.close();
            return;
        }

        // 提取 token（假设格式为 /socket/123456）
        String[] parts = uri.split("/");
        if (parts.length < 3) {
            sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
            ctx.close();
            return;
        }

        String token = parts[2];

        // 校验 Token（示例：查询 Redis 是否存在）
        if (!isValidToken(token)) {
            sendHttpResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED);
            ctx.close();
            return;
        }

        // Token 合法，继续 WebSocket 握手流程
        ctx.fireChannelRead(request.retain());
    }

    private boolean isValidToken(String token) {
        // 示例：Redis 中存储 key: token:123456
        return Boolean.TRUE;
//        Boolean exists = redisTemplate.hasKey("token:" + token);
//        return Boolean.TRUE.equals(exists);
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        HttpUtil.setContentLength(res, 0);
        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ChannelFuture f = ctx.writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || status.code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}