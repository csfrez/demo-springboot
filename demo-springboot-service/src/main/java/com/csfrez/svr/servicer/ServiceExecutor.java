package com.csfrez.svr.servicer;

import com.csfrez.svr.dto.SerResult;
import com.csfrez.svr.lambda.SerialBiFunction;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 执行Service方法的类，统一打日志、处理异常
 *
 * @author
 * @date 2026/1/27 14:27
 * @email
 */
@Slf4j
@Setter
public class ServiceExecutor<T, U, R> {
    private SerialBiFunction<T, U, R> serviceFn;  // 要执行的Lambda方法
    private U param;                               // 方法参数
    private ServiceManager.LambdaMeta<T> lambdaMeta;  // Service信息

    // 执行方法的核心逻辑
    public SerResult<R> callService() {
        // 记录开始时间，方便算耗时
        long startTime = System.currentTimeMillis();
        String serviceName = lambdaMeta.getClazz().getSimpleName();  // 比如UserService
        String methodName = lambdaMeta.getServiceName();             // 比如queryUser
        log.info("开始调用：{}的{}方法，参数：{}", serviceName, methodName, param);
        try {
            // 真正执行方法：用Service实例调用Lambda方法
            R result = serviceFn.apply(lambdaMeta.getInst(), param);
            // 算耗时，打成功日志
            long costTime = System.currentTimeMillis() - startTime;
            log.info("调用成功：{}的{}方法，耗时{}ms，结果：{}",
                    serviceName, methodName, costTime, result);
            // 返回成功结果
            return SerResult.success(result);
        } catch (Exception e) {
            // 出错了就打错误日志，返回失败结果
            long costTime = System.currentTimeMillis() - startTime;
            log.error("调用失败：{}的{}方法，耗时{}ms",
                    serviceName, methodName, costTime, e);
            return SerResult.fail("调用" + serviceName + "的" + methodName + "方法失败：" + e.getMessage());
        }
    }
}