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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author
 * @date 2025/5/30 15:03
 * @email
 */
//@Component
@ChannelHandler.Sharable
@Slf4j
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private TokenService tokenService;

    private static final String PATH_PREFIX = "/socket/";

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("WebSocketFrameHandler.userEventTriggered: {}", evt);
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String uri = handshake.requestUri();
            String token = extractToken(uri);
            if (token == null || !tokenService.validateToken(token)) {
                ctx.close();
                return;
            }

        } else if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new TextWebSocketFrame("PING PING PING"));
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private String extractToken(String uri) {
        if (uri.startsWith(PATH_PREFIX)) {
            return uri.substring(PATH_PREFIX.length());
        }
        return "123456";
    }

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