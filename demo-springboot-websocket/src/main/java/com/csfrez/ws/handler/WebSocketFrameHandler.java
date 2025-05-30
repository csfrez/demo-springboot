package com.csfrez.ws.handler;

import com.csfrez.ws.model.MyMessage;
import com.csfrez.ws.servie.ConnectionService;
import com.csfrez.ws.servie.MessageSenderService;
import com.csfrez.ws.servie.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/5/30 15:03
 * @email
 */
@Component
@ChannelHandler.Sharable
@Slf4j
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private TokenService tokenService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
//        log.info("WebSocketFrameHandler.channelRead0");
        String text = frame.text();
        log.info("WebSocketFrameHandler.channelRead0: {}", text);
        if ("PING".equals(text)) {
            ctx.writeAndFlush(new TextWebSocketFrame("PONG"));
        } else {
            // 解析Token（假设消息格式为JSON: {"token": "abc", "content": "msg"}）
            try {
                JsonNode json = new ObjectMapper().readTree(text);
                String token = json.get("token").asText();
                String content = json.get("content").asText();

                if (tokenService.validateToken(token)) {
                    connectionService.addConnection(token, ctx.channel());
                    messageSenderService.sendMessage("websocket_queue", new MyMessage(token, content));
                } else {
                    ctx.writeAndFlush(new TextWebSocketFrame("Invalid Token"));
                    ctx.close();
                }
            } catch (Exception e) {
                log.error("Error channelRead0", e);
                ctx.writeAndFlush(new TextWebSocketFrame("Invalid Message Format"));
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("WebSocketFrameHandler.handlerAdded");
//        String token = ctx.channel().attr(AttributeKey.valueOf("token")).get().toString();
//        connectionService.addConnection(token, ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        log.info("WebSocketFrameHandler.handlerRemoved");
//        String token = ctx.channel().attr(AttributeKey.valueOf("token")).get().toString();
//        connectionService.removeConnection(token, ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocketFrameHandler.exceptionCaught", cause);
        ctx.close();
    }
}