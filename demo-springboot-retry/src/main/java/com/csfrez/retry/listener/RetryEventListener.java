package com.csfrez.retry.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

/**
 * @author
 * @date 2025/5/23 16:10
 * @email
 */
@Slf4j
public class RetryEventListener extends RetryListenerSupport{


    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("Retry attempt {} failed: {}", context.getRetryCount(), throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable != null) {
            log.warn("All retries failed after {} attempts", context.getRetryCount());
        } else {
            log.info("Successfully completed after {} attempts", context.getRetryCount());
        }
    }
}
