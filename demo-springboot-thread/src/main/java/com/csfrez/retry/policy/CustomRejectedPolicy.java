package com.csfrez.retry.policy;

/**
 * @author
 * @date 2025/10/30 9:40
 * @email
 */

import com.csfrez.retry.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class CustomRejectedPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        // 1. 记录日志
        log.warn("Task " + task + " rejected from " + executor);

        // 2. 可选：保存任务以便后续处理（例如后台重试）
        try {
            ThreadUtil.pushRejectedTask(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while storing rejected task: " + e.getMessage());
        }

        // 3. 可选：触发告警、写入数据库、发送消息等
        // sendAlert("Thread pool overloaded, task rejected: " + r);

        // 4. 注意：这里不执行任务（不像 CallerRunsPolicy），只是处理拒绝逻辑
    }
}