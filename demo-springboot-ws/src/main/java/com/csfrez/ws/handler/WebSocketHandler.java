package com.csfrez.ws.handler;

import com.csfrez.ws.model.MyMessage;
import com.csfrez.ws.service.ConnectionService;
import com.csfrez.ws.service.MessageSenderService;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.Map;

/**
 * @author
 * @date 2025/6/4 9:13
 * @email
 */
@ServerEndpoint(path = "/socket/{token}", port = "${ws.port}", readerIdleTimeSeconds = "${ws.readerIdleTimeSeconds}", writerIdleTimeSeconds = "${ws.writerIdleTimeSeconds}")
@Slf4j
public class WebSocketHandler {

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private ConnectionService connectionService;

    @BeforeHandshake
    public void handshake(Session session, HttpHeaders headers, @RequestParam String gray, @RequestParam MultiValueMap reqMap, @PathVariable String token, @PathVariable Map pathMap) {
        log.info("handshake id: {}, gray: {}, reqMap: {}, token: {}, pathMap: {}", session.id().toString(), gray, reqMap, token, pathMap);
        log.info("handshake: idShortText {}, idLongText {}", session.id().asShortText(), session.id().asLongText());
//        session.setSubprotocols("stomp");
        if (!"123456".equals(token)) {
            System.out.println("Authentication failed!");
            session.close();
        }
    }

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @RequestParam String gray, @RequestParam MultiValueMap reqMap, @PathVariable String token, @PathVariable Map pathMap) {
        log.info("onOpen id: {}, gray: {}, reqMap: {}, token: {}, pathMap: {}", session.id().toString(), gray, reqMap, token, pathMap);
//        ConnectionUtil.addUser(token, session.id().toString());
//        ConnectionUtil.addSession(session.id().toString(), session);
        String sessionId = session.id().toString();
        connectionService.openSession(sessionId, token, session);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        log.info("onClose: {}", session.id());
        String sessionId = session.id().toString();
        connectionService.closeSession(sessionId);
//        ConnectionUtil.removeSession(session.id().toString());
//        String userId = ConnectionUtil.getUserId(session.id().toString());
//        if (StrUtil.isNotBlank(userId)) {
//            ConnectionUtil.removeUser(userId);
//        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("onError: " + session.id(), throwable);
        String sessionId = session.id().toString();
        connectionService.closeSession(sessionId);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("onMessage id: {}, message: {}", session.id(), message);
        if ("PING".equals(message)) {
            session.sendText("PONG");
            return;
        }
        if ("PONG".equals(message)) {
            return;
        }
        if ("CLOSED".equals(message)) {
            session.close();
            return;
        }
        message = message + " from server " + System.currentTimeMillis();
        messageSenderService.sendMessage("websocket_queue", new MyMessage("123456", message));
//        String newMessage = message + " from server";
//        session.sendText(newMessage);
    }

    @OnBinary
    public void onBinary(Session session, byte[] bytes) {
        for (byte b : bytes) {
            log.info("onBinary id: {}, byte: {}", session.id(), b);
        }
        session.sendBinary(bytes);
    }

    @OnEvent
    public void onEvent(Session session, Object evt) {
        log.info("onEvent id: {}, evt: {}", session.id(), evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    log.info("read idle");
                    //session.sendText("PING");
                    break;
                case WRITER_IDLE:
                    log.info("write idle");
                    session.sendText("PING");
                    break;
                case ALL_IDLE:
                    log.info("all idle");
                    break;
                default:
                    break;
            }
        }
    }
}
