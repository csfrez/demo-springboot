package com.csfrez.ai.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/6/19 17:22
 * @email
 */
@Component
@Slf4j
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {


    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.warn("Spring 上下文已关闭，准备执行最终清理...");
    }
}
