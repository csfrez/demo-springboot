package com.csfrez.ws.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author
 * @date 2025/5/30 10:17
 * @email
 */
//@Component
//@RequiredArgsConstructor
@ChannelHandler.Sharable
@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private static final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

    public WebSocketHandler(RedisTemplate<String, String> redisTemplate, RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        log.info("Received message: {}", text);
        if ("PING".equals(text)) {
            ctx.writeAndFlush(new TextWebSocketFrame("PONG"));
        } else {
            // 解析Token（假设消息格式为JSON: {"token": "abc", "content": "msg"}）
            try {
                JsonNode json = new ObjectMapper().readTree(text);
                String token = json.get("token").asText();
                String content = json.get("content").asText();

                if (isValidToken(token)) {
                    CHANNEL_MAP.put(token, ctx.channel());
                    sendMessage(token, content);
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

    private boolean isValidToken(String token) {
        redisTemplate.hasKey("token:" + token);
        return Boolean.TRUE;
//        return Boolean.TRUE.equals(redisTemplate.hasKey("token:" + token));
    }

    public void sendMessage(String targetToken, String message) {
        Channel targetChannel = CHANNEL_MAP.get(targetToken);
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.writeAndFlush(new TextWebSocketFrame(message));
        } else {
            rabbitTemplate.convertAndSend("socket." + targetToken, message);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("WebSocketHandler handlerAdded");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        log.info("WebSocketHandler handlerRemoved");
        CHANNEL_MAP.values().removeIf(channel -> channel == ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.info("WebSocketHandler userEventTriggered");
        if (evt instanceof IdleStateEvent) {
            ctx.close();
        }
    }

}