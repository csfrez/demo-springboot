package com.csfrez.demospringboot.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


@Aspect
@Slf4j
@Component
public class WebLogAspect {

    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void cutController() {
    }

    @Before("cutController()")
    public void doBefore(JoinPoint point) {
        //获取拦截方法的参数
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String url = request.getRequestURL().toString();
        List<Object> list = Lists.newArrayList();
        for (Object object : point.getArgs()) {
            if (object instanceof MultipartFile || object instanceof HttpServletRequest || object instanceof HttpServletResponse || object instanceof BindingResult) {
                continue;
            }
            list.add(object);
        }
        log.info("请求 url:[{}], params:[{}]", url, StrUtil.join(",", list));
    }

    /**
     * 返回通知：
     * 1. 在目标方法正常结束之后执行
     * 1. 在返回通知中补充请求日志信息，如返回时间，方法耗时，返回值，并且保存日志信息
     *
     * @param response
     * @throws Throwable
     */
    @AfterReturning(returning = "response", pointcut = "cutController()")
    public void doAfterReturning(Object response) {
        if (response != null) {
            log.info("请求返回result:[{}]", JSONUtil.toJsonStr(response));
        }
    }
}