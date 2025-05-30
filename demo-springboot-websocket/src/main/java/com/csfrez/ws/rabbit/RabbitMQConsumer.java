package com.csfrez.ws.rabbit;

import com.csfrez.ws.handler.WebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/5/30 10:21
 * @email
 */
//@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumer {

    private final WebSocketHandler webSocketHandler;

    @RabbitListener(queues = "socket_queue")
    public void processMessage(String message) {
        try {
            JsonNode json = new ObjectMapper().readTree(message);
            String token = json.get("token").asText();
            String content = json.get("content").asText();
            webSocketHandler.sendMessage(token, content);
        } catch (Exception e) {
            // 处理异常
            log.error("Error processMessage", e);
        }
    }
}
