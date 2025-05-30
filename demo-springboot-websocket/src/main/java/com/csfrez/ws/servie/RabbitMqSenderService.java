package com.csfrez.ws.servie;

import com.csfrez.ws.model.MyMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/30 14:52
 * @email
 */
@Service
@Slf4j
public class RabbitMqSenderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendToAllNodes(MyMessage message) {
        try {
            rabbitTemplate.convertAndSend("websocket_exchange", "", message);
        } catch (Exception e){
            log.error("send message to all nodes error", e);
        }
    }
}
