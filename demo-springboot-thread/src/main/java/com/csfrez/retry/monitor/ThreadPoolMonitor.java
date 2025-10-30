package com.csfrez.retry.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author
 * @date 2025/10/30 9:46
 * @email
 */
@Component
@Slf4j
public class ThreadPoolMonitor {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

//    @Scheduled(fixedRate = 1000)
    public void monitor() {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
        log.info("{}线程池状态: 活跃线程={}, 最大线程={}, 队列大小={}", taskExecutor.getThreadNamePrefix(),
                executor.getActiveCount(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size());
    }
}
