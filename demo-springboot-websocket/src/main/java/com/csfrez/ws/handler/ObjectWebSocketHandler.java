package com.csfrez.ws.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author
 * @date 2025/6/3 11:49
 * @email
 */
public class ObjectWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 关闭请求处理
        if (frame instanceof CloseWebSocketFrame) {
            ctx.close();
            return;
        }

        // 心跳PING处理
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 只处理文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException("Unsupported frame type: " + frame.getClass().getName());
        }

        // 业务消息处理
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("Received: " + request);

        // 示例：返回处理结果
        ctx.channel().writeAndFlush(new TextWebSocketFrame("Echo: " + request));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 心跳检测处理
        if (evt instanceof IdleStateEvent) {
            ctx.close();
            System.out.println("Connection closed due to idle timeout");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}