package com.csfrez.retry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @author
 * @date 2025/5/23 15:03
 * @email
 */
@Service
@Slf4j
public class HelloService {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public String hello() {
        threadPoolTaskExecutor.execute(() -> {
            log.warn("hello ==> " + Thread.currentThread().getName());
        });
        return null;
    }

    public void hello(Integer count) {
        for (int i = 0; i < count; i++) {
            int index = i;
            threadPoolTaskExecutor.execute(() -> {
                Random random = new Random();
                int sleepTime = random.nextInt(5000);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    log.error("InterruptedException", e);
                }
                log.info("{}. {}==>{}", index, Thread.currentThread().getName(), sleepTime);
            });
        }
    }
}
