package com.csfrez.demospringboot.filter;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 调用其他dubbo服务日志拦截器
 *
 */
@Activate(group = Constants.CONSUMER, order = 200000)
public class ConsumerLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerLoggingFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String serviceString = url.toServiceString();
        boolean monitorServiceFlag = serviceString.contains(MonitorService.class.getName());
        if (!monitorServiceFlag) {
            try {
                logger.info("发送请求[interface={}${}, args={}]", new Object[] {serviceString, invocation.getMethodName(), invocation.getArguments()});
            } catch (Exception ignore) {
            }
        }
        Result result = invoker.invoke(invocation);
        if (!monitorServiceFlag && result != null) {
            try {
                logger.info("接受结果:[{}]", new Object[] {result.getValue()});
            } catch (Exception ignore) {
            }
        }
        return result;
    }
}
