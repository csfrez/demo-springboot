package com.csfrez.demospringboot.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.csfrez.demospringboot.dubbo.DemoService;

@RestController
public class DemoConsumerController {

    @Reference(version = "1.0.0", application = "${dubbo.application.id}")
    private DemoService demoService;

    @RequestMapping("/sayHello")
    public String sayHello(@RequestParam String name) {
        return demoService.sayHello(name);
    }
    
    @GetMapping("/hello")
    public String hello() {       
        return "Hello Spring Security";
    }
}
