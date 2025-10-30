package com.csfrez.retry.pool;

import org.slf4j.MDC;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @date 2024/9/26 10:38
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {


    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull ThreadFactory threadFactory, @NotNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable task) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(wrap(task, context));
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(wrap(callable, context));
    }

    @Override
    public void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
    }

    @Override
    public void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
    }

    public static Runnable wrap(Runnable task, Map<String, String> context) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                // 将父线程的MDC内容传给子线程
                MDC.setContextMap(context);
            }
            try {
                // 执行异步操作
                task.run();
            } finally {
                if (previous != null) {
                    // 还原之前的MDC内容
                    MDC.setContextMap(previous);
                } else {
                    // 清空MDC内容
                    MDC.clear();
                }
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> task, Map<String, String> context) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                // 将父线程的MDC内容传给子线程
                MDC.setContextMap(context);
            }
            try {
                // 执行异步操作
                return task.call();
            } finally {
                if (previous != null) {
                    // 还原之前的MDC内容
                    MDC.setContextMap(previous);
                } else {
                    // 清空MDC内容
                    MDC.clear();
                }
            }
        };
    }
}
