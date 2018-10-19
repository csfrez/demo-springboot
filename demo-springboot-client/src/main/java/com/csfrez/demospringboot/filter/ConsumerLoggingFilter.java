package com.csfrez.demospringboot.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

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
                logger.info("发送请求[interfact={}${}, args={}]", new Object[] {serviceString, invocation.getMethodName(), invocation.getArguments()});
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
