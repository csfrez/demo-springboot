package com.csfrez.redis.lock;

import cn.hutool.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author
 * @date 2025/7/29 10:40
 * @email
 */
public class HelloTest {

    @Test
    public void testRedisLock() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String result = HttpUtil.get("http://localhost:8080/redis/123456");
                    System.out.println(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
    }

    @Test
    public void testRedissonLock() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String result = HttpUtil.get("http://localhost:8080/redisson/123456");
                    System.out.println(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
    }
}
