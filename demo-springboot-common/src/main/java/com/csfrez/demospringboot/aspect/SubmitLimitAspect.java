package com.csfrez.demospringboot.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.csfrez.demospringboot.annotation.SubmitLimit;
import com.csfrez.demospringboot.base.ResponseResult;
import com.csfrez.demospringboot.exception.CommonException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Objects;

@Order(1)
@Aspect
@Component
public class SubmitLimitAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitLimitAspect.class);

    /**
     * redis分割符
     */
    private static final String REDIS_SEPARATOR = ":";

    /**
     * 默认锁对应的值
     */
    private static final String DEFAULT_LOCK_VALUE = "DEFAULT_SUBMIT_LOCK_VALUE";

    /**
     * 默认重复提交提示语
     */
    private static final String DEFAULT_TIP_MSG = "服务正在处理，请勿重复提交！";

    @Value("${spring.application.name}")
    private String applicationName;

//    @Autowired
//    private RedisLockService redisLockService;


    /**
     * 方法调用环绕拦截
     */
    @Around(value = "@annotation(com.csfrez.demospringboot.annotation.SubmitLimit)")
    public Object doAround(ProceedingJoinPoint joinPoint) {
        HttpServletRequest request = getHttpServletRequest();
        if (Objects.isNull(request)) {
            return ResponseResult.failed("请求参数不能为空！");
        }
        //获取注解配置的参数
        SubmitLimit submitLimit = getSubmitLimit(joinPoint);
        //组合生成key，通过key实现加锁和解锁
        String lockKey = buildSubmitLimitKey(joinPoint, request, submitLimit.customerHeaders());
        //尝试在指定的时间内加锁
//        boolean lock = redisLockService.tryLock(lockKey, DEFAULT_LOCK_VALUE, Duration.ofMillis(submitLimit.waitTime()));
        boolean lock = true;
        if (!lock) {
            String tipMsg = StrUtil.isEmpty(submitLimit.customerTipMsg()) ? DEFAULT_TIP_MSG : submitLimit.customerTipMsg();
            return ResponseResult.failed(tipMsg);
        }
        try {
            //继续执行后续流程
            return execute(joinPoint);
        } finally {
            //执行完毕之后，手动将锁释放
//            redisLockService.releaseLock(lockKey, DEFAULT_LOCK_VALUE);
        }
    }

    /**
     * 执行任务
     *
     * @param joinPoint
     * @return
     */
    private Object execute(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (CommonException e) {
            return ResponseResult.failed(e.getMessage());
        } catch (Throwable e) {
            LOGGER.error("业务处理发生异常，错误信息：", e);
            return ResponseResult.failed(e.getMessage());
        }
    }


    /**
     * 获取请求对象
     *
     * @return
     */
    private HttpServletRequest getHttpServletRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();
        return request;
    }

    /**
     * 获取注解值
     *
     * @param joinPoint
     * @return
     */
    private SubmitLimit getSubmitLimit(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        SubmitLimit submitLimit = method.getAnnotation(SubmitLimit.class);
        return submitLimit;
    }

    /**
     * 组合生成lockKey
     * 生成规则：项目名+接口名+方法名+请求参数签名（对请求头部参数+请求body参数，取SHA1值）
     *
     * @param joinPoint
     * @param request
     * @param customerHeaders
     * @return
     */
    private String buildSubmitLimitKey(JoinPoint joinPoint, HttpServletRequest request, String[] customerHeaders) {
        //请求参数=请求头部+请求body
        String requestHeader = getRequestHeader(request, customerHeaders);
        String requestBody = getRequestBody(joinPoint.getArgs());
        String requestParamSign = DigestUtil.sha1Hex(requestHeader + requestBody);
        String submitLimitKey = new StringBuilder()
                .append(applicationName)
                .append(REDIS_SEPARATOR)
                .append(joinPoint.getSignature().getDeclaringType().getSimpleName())
                .append(REDIS_SEPARATOR)
                .append(joinPoint.getSignature().getName())
                .append(REDIS_SEPARATOR)
                .append(requestParamSign)
                .toString();
        return submitLimitKey;
    }


    /**
     * 获取指定请求头部参数
     *
     * @param request
     * @param customerHeaders
     * @return
     */
    private String getRequestHeader(HttpServletRequest request, String[] customerHeaders) {
        if (Objects.isNull(customerHeaders)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String headerKey : customerHeaders) {
            sb.append(request.getHeader(headerKey));
        }
        return sb.toString();
    }


    /**
     * 获取请求body参数
     *
     * @param args
     * @return
     */
    private String getRequestBody(Object[] args) {
        if (Objects.isNull(args)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof BindResult
                    || arg instanceof MultipartFile[]
                    || arg instanceof ModelMap
                    || arg instanceof Model
                    || arg instanceof ExtendedServletRequestDataBinder
                    || arg instanceof byte[]) {
                continue;
            }
            sb.append(JSONUtil.toJsonStr(arg));
        }
        return sb.toString();
    }
}