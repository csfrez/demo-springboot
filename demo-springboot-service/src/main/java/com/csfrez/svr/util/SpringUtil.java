package com.csfrez.svr.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 从Spring里拿Bean的工具，不用自己注入Service
 *
 * @author
 * @date 2026/1/27 11:54
 * @email
 */
@Component
public class SpringUtil implements ApplicationContextAware {
    // Spring的上下文，相当于“Bean仓库”
    private static ApplicationContext applicationContext;

    // 从仓库里按类型拿Bean，比如拿UserService类型的实例
    public static <T> T getBean(Class<T> requiredType) {
        if (applicationContext == null) {
            throw new RuntimeException("Spring还没初始化好呢！");
        }
        return applicationContext.getBean(requiredType);
    }

    // 下面这行是Spring自动调用的，不用管
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }
}
