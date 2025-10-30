package com.csfrez.retry.config;

import com.csfrez.retry.policy.CustomCallerRunsPolicy;
import com.csfrez.retry.pool.CustomThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author
 * @date 2025/5/23 16:10
 * @email
 */
@Configuration
public class HelloConfig {


    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CustomThreadPoolTaskExecutor customThreadPoolTaskExecutor() {
        CustomThreadPoolTaskExecutor threadPoolExecutor = new CustomThreadPoolTaskExecutor();
        // 线程池最大活跃的线程数
        threadPoolExecutor.setMaxPoolSize(5);
        // 线程池活跃的线程数
        threadPoolExecutor.setCorePoolSize(2);
        // 队列的最大容量
        threadPoolExecutor.setQueueCapacity(10);
        // 线程池维护线程所允许的空闲时间
        threadPoolExecutor.setKeepAliveSeconds(500);
        threadPoolExecutor.setThreadNamePrefix("common-");
//        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        threadPoolExecutor.setRejectedExecutionHandler(new CustomRejectedPolicy());
        threadPoolExecutor.setRejectedExecutionHandler(new CustomCallerRunsPolicy());
        threadPoolExecutor.initialize();
        // 设置为守护进程  主线程结束，则task线程也结束
        threadPoolExecutor.setDaemon(true);
        return threadPoolExecutor;
    }
}
