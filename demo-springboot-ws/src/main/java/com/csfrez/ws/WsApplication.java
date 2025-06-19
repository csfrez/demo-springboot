package com.csfrez.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.yeauty.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
public class WsApplication  {

    public static void main(String[] args) {
        SpringApplication.run(WsApplication.class, args);
    }

}
