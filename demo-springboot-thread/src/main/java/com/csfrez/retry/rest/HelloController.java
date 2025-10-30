package com.csfrez.retry.rest;

import com.csfrez.retry.service.HelloService;
import com.csfrez.retry.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.BlockingQueue;

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
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok(helloService.hello());
    }

    @GetMapping("/hello/{count}")
    public ResponseEntity<String> helloCount(@PathVariable("count") Integer count){
        helloService.hello(count);
        return ResponseEntity.ok("");
    }

    @GetMapping("/retry")
    public ResponseEntity<String> retry(){
        BlockingQueue<Runnable> rejectedTasks = ThreadUtil.getRejectedTasks();
        log.info("rejectedTasks size: {}", rejectedTasks.size());
        while (!rejectedTasks.isEmpty()) {
            Runnable task = rejectedTasks.poll();
            log.info("task: {}", task);
            task.run();
        }
        return ResponseEntity.ok("");
    }
}
