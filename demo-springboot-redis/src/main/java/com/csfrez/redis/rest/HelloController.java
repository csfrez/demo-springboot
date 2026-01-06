package com.csfrez.redis.rest;

import cn.hutool.core.util.RandomUtil;
import com.csfrez.redis.lock.BaseRedisLock;
import com.csfrez.redis.lock.BaseRedissonLock;
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

    private static final BaseRedisLock REDIS_LOCK = new BaseRedisLock("REDIS_LOCK");

    private static final BaseRedissonLock REDISSON_LOCK = new BaseRedissonLock("REDISSON_LOCK");

    @GetMapping("/redis/{identity}")
    public String redis(@PathVariable String identity) {
        int randomInt = RandomUtil.randomInt(1, 5);
        REDIS_LOCK.consume(identity, () -> {
            try {
                log.info("redisredis: {}", randomInt);
                long start = System.currentTimeMillis();
                TimeUnit.SECONDS.sleep(randomInt);
                log.info("redis: {} cost: {}", randomInt, System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                log.error("redis: ", e);
            }
            // throw new RuntimeException("抛出异常");
        });
        return "redis world " + randomInt;
    }

    @GetMapping("/redisson/{identity}")
    public String redisson(@PathVariable String identity) {
        int randomInt = RandomUtil.randomInt(1, 5);
        REDISSON_LOCK.consume(identity, () -> {
            try {
                log.info("redisson: {}", randomInt);
                long start = System.currentTimeMillis();
                TimeUnit.SECONDS.sleep(randomInt);
                log.info("redisson: {} cost: {}", randomInt, System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                log.error("redisson: ", e);
            }
        });
        return "redisson world " + randomInt;
    }
}
