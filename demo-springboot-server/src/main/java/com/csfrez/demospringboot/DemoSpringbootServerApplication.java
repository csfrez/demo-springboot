package com.csfrez.demospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.csfrez.demospringboot.mapper")
public class DemoSpringbootServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoSpringbootServerApplication.class, args);
	}
}
