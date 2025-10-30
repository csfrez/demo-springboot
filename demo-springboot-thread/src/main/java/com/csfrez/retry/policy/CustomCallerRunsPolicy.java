package com.csfrez.retry.policy;

import cn.hutool.core.util.StrUtil;
import com.csfrez.retry.util.DingAlertUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author
 * @date 2025/10/30 10:29
 * @email
 */
@Slf4j
public class CustomCallerRunsPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        log.warn("Task: {}, rejected from: {}", task, executor);
        try {
            String alertPrompt = StrUtil.format("告警，{}线程池已满，任务执行过载： {}", executor, task) ;
            DingAlertUtil.robotAlert(alertPrompt);
        } catch (Exception e){
            log.error("告警信息发送失败", e);
        }
        if (!executor.isShutdown()) {
            task.run();
        }
    }
}
