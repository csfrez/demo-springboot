package com.csfrez.svr.rest;

import com.csfrez.svr.dto.SerResult;
import com.csfrez.svr.service.HelloService;
import com.csfrez.svr.servicer.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author
 * @date 2025/5/23 11:52
 * @email
 */
@Slf4j
@RestController
public class HelloController {

    @Autowired
    private HelloService helloService;

    @GetMapping("/hello")
    public ResponseEntity<String> hello(@RequestParam(defaultValue = "Peter") String name) {
        SerResult<String> call = ServiceManager.call(HelloService::hello, name);
        return ResponseEntity.ok(call.getData());
    }

}
