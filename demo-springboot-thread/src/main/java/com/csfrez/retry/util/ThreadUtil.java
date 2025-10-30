package com.csfrez.retry.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * @date 2025/10/30 10:01
 * @email
 */
@Slf4j
public class ThreadUtil {

    // 可选：保存被拒绝的任务（注意线程安全）
    private static final BlockingQueue<Runnable> rejectedTasks = new LinkedBlockingQueue<>();

    // 提供方法供外部获取被拒绝的任务（例如后台线程重试）
    public static BlockingQueue<Runnable> getRejectedTasks() {
        return rejectedTasks;
    }

    public static void pushRejectedTask(Runnable task) throws InterruptedException {
        // 设置超时避免无限阻塞（比如最多等1秒）
        if (!rejectedTasks.offer(task, 1, TimeUnit.SECONDS)) {
            log.warn("Failed to store rejected task: " + task);
            // 如果队列也满了，可以选择丢弃或进一步处理
        }
        rejectedTasks.offer(task);
    }
}
