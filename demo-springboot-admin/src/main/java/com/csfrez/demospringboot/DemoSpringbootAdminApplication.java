package com.csfrez.demospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.codecentric.boot.admin.config.EnableAdminServer;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAdminServer
public class DemoSpringbootAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoSpringbootAdminApplication.class, args);
	}
}
