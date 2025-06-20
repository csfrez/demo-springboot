package com.csfrez.token.handler;

import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author
 * @date 2025/6/20 14:52
 * @email
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // 全局异常拦截
    @ExceptionHandler
    public SaResult handlerException(Exception e) {
        log.error("全局异常拦截", e);
        return SaResult.error(e.getMessage());
    }
}
