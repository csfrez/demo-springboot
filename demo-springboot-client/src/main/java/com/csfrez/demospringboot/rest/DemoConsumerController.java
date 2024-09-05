package com.csfrez.demospringboot.rest;

import com.csfrez.demospringboot.dubbo.DemoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoConsumerController {

    @DubboReference
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
