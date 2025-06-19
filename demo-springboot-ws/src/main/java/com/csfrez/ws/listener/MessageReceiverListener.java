package com.csfrez.ws.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.csfrez.ws.model.MyMessage;
import com.csfrez.ws.service.ConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yeauty.pojo.Session;

import java.util.List;
import java.util.Objects;

/**
 * @author
 * @date 2025/5/30 16:28
 * @email
 */
@Component
@Slf4j
public class MessageReceiverListener {

    @Autowired
    private ConnectionService connectionService;

    @RabbitListener(queues = "websocket_queue")
    public void receive(MyMessage myMessage) {
        try {
            log.info("MessageReceiverListener.Received message {}", JSONUtil.toJsonStr(myMessage));
            List<String> sessionIdList = connectionService.getSessionIdList(myMessage.getUserId());
            if (CollUtil.isEmpty(sessionIdList)) {
                log.warn("User {} not online", myMessage.getUserId());
                return;
            }
            sessionIdList.forEach(sessionId -> {
                Session session = connectionService.getSession(sessionId);
                if (Objects.nonNull(session) && session.isActive()) {
                    session.sendText(myMessage.getContent());
                } else {
                    log.warn("Session {} not active", sessionId);
                    connectionService.
                            removeSessionId(myMessage.getUserId(), sessionId);
                    connectionService.removeSession(sessionId);
                }
            });
        } catch (Exception e) {
            log.error("Receive message error: " + e.getMessage());
        }
    }
}
