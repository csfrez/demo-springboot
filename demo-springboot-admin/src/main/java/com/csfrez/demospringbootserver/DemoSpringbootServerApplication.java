package com.csfrez.demospringbootserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.codecentric.boot.admin.server.config.EnableAdminServer;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAdminServer
public class DemoSpringbootServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoSpringbootServerApplication.class, args);
	}
}
