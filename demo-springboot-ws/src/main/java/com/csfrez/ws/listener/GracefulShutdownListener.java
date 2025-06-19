package com.csfrez.ws.listener;

import cn.hutool.core.collection.CollUtil;
import com.csfrez.ws.service.ConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author
 * @date 2025/6/19 17:22
 * @email
 */
@Component
@Slf4j
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    private ConnectionService connectionService;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.warn("Spring 上下文已关闭，准备执行最终清理...");
        Map<String, String> userIdMap = connectionService.getUserIdMap();
        userIdMap.forEach((sessionId, userId) -> {
            connectionService.removeSessionId(userId, sessionId);
        });

        Set<String> userIdSet = connectionService.getUserIdSet();
        userIdSet.forEach(userId -> {
            List<String> sessionIdList = connectionService.getSessionIdList(userId);
            if (CollUtil.isEmpty(sessionIdList)) {
                connectionService.deleteUserId(userId);
            }
        });
    }
}
