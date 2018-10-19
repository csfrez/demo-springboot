package com.csfrez.demospringboot.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

@Activate(group = Constants.PROVIDER, order = -200000)
public class ProviderLoggingFilter implements Filter {
	
    private static final Logger logger = LoggerFactory.getLogger(ProviderLoggingFilter.class);

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            logger.info("接受请求:[ip={}, inferface={}${}, args={}]", new Object[] {RpcContext.getContext().getRemoteAddressString(), invoker.getUrl().getServiceInterface(), invocation.getMethodName(), invocation.getArguments()});
        } catch (Exception ignore) {
        }
        Result result = invoker.invoke(invocation);
        try {
            logger.info("返回结果:{}", new Object[] {result.getValue()});
        } catch (Exception ignore) {
        }
        return result;
    }
}