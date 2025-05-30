package com.csfrez.ws.listener;

import com.csfrez.ws.model.MyMessage;
import com.csfrez.ws.servie.ConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            log.info("MessageReceiverListener.Received message {}", new ObjectMapper().writeValueAsString(myMessage));
//            String msg = new String(message);
//            ObjectMapper objectMapper = new ObjectMapper();
//            MyMessage myMessage = objectMapper.readValue(msg, MyMessage.class);
            Channel channel = connectionService.getChannel(myMessage.getToken());
            if (channel != null && channel.isActive()){
                channel.writeAndFlush(new TextWebSocketFrame(myMessage.getContent()));
            }
        } catch (Exception e){
            log.error("Receive message error: " + e.getMessage());
        }
    }
}
