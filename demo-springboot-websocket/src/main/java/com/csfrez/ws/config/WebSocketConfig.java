package com.csfrez.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/5/30 10:14
 * @email
 */
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketConfig {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

//    @Bean
//    public Queue socketQueue() {
//        return new Queue("socket_queue", true); // 持久化队列
//    }
}