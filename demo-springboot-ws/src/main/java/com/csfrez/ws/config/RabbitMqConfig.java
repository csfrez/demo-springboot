package com.csfrez.ws.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author
 * @date 2025/5/30 14:59
 * @email
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue websocketQueue() {
        return new Queue("websocket_queue");
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
