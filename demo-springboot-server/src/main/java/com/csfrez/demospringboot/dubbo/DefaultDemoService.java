package com.csfrez.demospringboot.dubbo;


import org.apache.dubbo.config.annotation.Service;

@Service
public class DefaultDemoService implements DemoService {

	@Override
	public String sayHello(String name) {
		return "Hello, " + name + " (from Spring Boot), time is " + System.currentTimeMillis();
	}
}
