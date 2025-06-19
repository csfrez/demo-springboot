package com.csfrez.ws.rest;

import cn.hutool.core.lang.Pair;
import com.csfrez.ws.model.MyMessage;
import com.csfrez.ws.service.ConnectionService;
import com.csfrez.ws.service.MessageSenderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeauty.pojo.Session;

import java.util.Map;

/**
 * @author
 * @date 2025/6/19 10:35
 * @email
 */
@Slf4j
@RestController
@AllArgsConstructor
public class WebSocketController {

    private MessageSenderService messageSenderService;

    private ConnectionService connectionService;

    @GetMapping("/hello")
    public String hello(@RequestParam String id) {
        Session session = connectionService.getSession(id);
        String message = "hello world ==> " + id + ", " + System.currentTimeMillis();
        if (session != null) {
            session.sendText(message);
        }
        return message;
    }

    @GetMapping("/send")
    public void send(@RequestParam String userId) {
        String message = "hello world ==> " + userId + ", " + System.currentTimeMillis();
        log.info("send message to userId: {}, message: {}", userId, message);
        MyMessage myMessage = new MyMessage(userId, message);
        messageSenderService.sendMessage("websocket_queue", myMessage);
    }

    @GetMapping("/count")
    public ResponseEntity<String> count(){
        Pair<Map<String, Session>, Map<String, String>> mapPair = connectionService.getAll();
        mapPair.getKey().forEach((sessionId, session) -> {
            log.info("sessionId: {}, session: {}", sessionId, session);
        });
        mapPair.getValue().forEach((sessionId, userId) -> {
            log.info("sessionId: {}, userId: {}", sessionId, userId);
        });
        return ResponseEntity.ok("ok");
    }
}
