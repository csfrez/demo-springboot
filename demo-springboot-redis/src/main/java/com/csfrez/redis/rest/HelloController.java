package com.csfrez.redis.rest;

import cn.hutool.core.util.RandomUtil;
import com.csfrez.redis.lock.BaseRedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author
 * @date 2025/7/28 9:22
 * @email
 */
@RestController
@Slf4j
public class HelloController {

    private static final BaseRedisLock HELLO_LOCK = new BaseRedisLock("HELLO_LOCK");

    @GetMapping("/hello/{identity}")
    public String hello(@PathVariable String identity) {
        int randomInt = RandomUtil.randomInt(50, 100);
        HELLO_LOCK.consume(identity, () -> {
            try {
                log.info("hello: {}", randomInt);
                long start = System.currentTimeMillis();
                TimeUnit.SECONDS.sleep(randomInt);
                log.info("hello: {} cost: {}", randomInt, System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                log.error("hello: ", e);
            }
        });
        return "hello world";
    }
}
