package com.csfrez.ws.service;

import com.csfrez.ws.model.MyMessage;
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

    public void sendMessage(String queueName, MyMessage myMessage) {
        // 可以自定义消息属性
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // 将对象转换为 JSON 字节流
        byte[] body = rabbitTemplate.getMessageConverter().toMessage(myMessage, props).getBody();
        Message message = new Message(body, props);
        // 发送到指定队列
        rabbitTemplate.send(queueName, message);
    }
}
