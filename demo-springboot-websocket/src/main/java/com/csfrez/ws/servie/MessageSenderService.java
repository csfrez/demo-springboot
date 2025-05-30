package com.csfrez.ws.servie;

import com.csfrez.ws.model.MyMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/30 15:58
 * @email
 */
@Service
public class MessageSenderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String queueName, MyMessage message) {
        // 可以自定义消息属性
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // 将对象转换为 JSON 字节流
        byte[] body = rabbitTemplate.getMessageConverter().toMessage(message, props).getBody();

        Message msg = new Message(body, props);
        rabbitTemplate.send(queueName, msg); // 发送到指定队列
    }
}
