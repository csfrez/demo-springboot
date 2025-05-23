package com.csfrez.retry.config;

import com.csfrez.retry.listener.RetryEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author
 * @date 2025/5/23 16:10
 * @email
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryEventListener retryEventListener() {
        return new RetryEventListener();
    }
}
