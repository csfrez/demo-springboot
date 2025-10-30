package com.csfrez.retry.pool;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @date 2024/9/26 11:16
 */
public class CustomThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private static final long serialVersionUID = 1L;

    private Runnable genRunnable(Runnable task, Map<String, String> mdcContext) {
        return () -> {
            // 将父线程的MDC内容传给子线程
            if (Objects.nonNull(mdcContext)) {
                MDC.setContextMap(mdcContext);
            }
            try {
                // 执行异步操作
                task.run();
            } finally {
                // 清空MDC内容
                MDC.clear();
            }
        };
    }

    private <T> Callable<T> genCallable(Callable<T> callable, Map<String, String> mdcContext) {
        return () -> {
            // 将父线程的MDC内容传给子线程
            if (Objects.nonNull(mdcContext)) {
                MDC.setContextMap(mdcContext);
            }
            try {
                // 执行异步操作
                return callable.call();
            } finally {
                // 清空MDC内容
                MDC.clear();
            }
        };
    }

    @Override
    public void execute(Runnable task) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(genRunnable(task, context));
    }
    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(genCallable(callable, context));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submitListenable(genRunnable(task, context));
    }
}
