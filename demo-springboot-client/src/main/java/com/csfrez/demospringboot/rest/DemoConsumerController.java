package com.csfrez.demospringboot.rest;

import com.csfrez.demospringboot.dubbo.DemoService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

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
    
    @GetMapping("/product/{id}")
    public String getProduct(@PathVariable String id) {
        //for debug
        return "product id : " + id;
    }
    
    @GetMapping("/order/{id}")
    public String getOrder(@PathVariable String id) {
        //for debug
        return "order id : " + id;
    }
}
