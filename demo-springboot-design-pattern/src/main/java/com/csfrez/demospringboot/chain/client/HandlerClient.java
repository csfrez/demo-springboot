package com.csfrez.demospringboot.chain.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.csfrez.demospringboot.chain.config.ProductCheckHandlerConfig;
import com.csfrez.demospringboot.chain.handler.AbstractCheckHandler;
import com.csfrez.demospringboot.chain.vo.ProductVO;
import com.csfrez.demospringboot.chain.dto.Result;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

@Component
public class HandlerClient {

    /**
     * 使用Spring注入:所有继承了AbstractCheckHandler抽象类的Spring Bean都会注入进来。Map的Key对应Bean的name,Value是name对应相应的Bean
     */
    @Resource
    private Map<String, AbstractCheckHandler> handlerMap;

    /**
     * 参数校验：责任链模式
     *
     * @param param
     * @return
     */
    public String paramCheck(ProductVO param) {

        //获取处理器配置：通常配置使用统一配置中心存储，支持动态变更
        ProductCheckHandlerConfig handlerConfig = this.getHandlerConfigFile();

        //获取处理器
        AbstractCheckHandler handler = this.getHandler(handlerConfig);

        //责任链：执行处理器链路
        Result executeChainResult = HandlerClient.executeChain(handler, param);
        if (!executeChainResult.isSuccess()) {
            System.out.println("创建商品 失败...");
            return executeChainResult.toString();
        }

        //处理器链路全部成功
        return Result.success().toString();
    }

    /**
     * 获取处理器配置：通常配置使用统一配置中心存储，支持动态变更
     *
     * @return
     */
    private ProductCheckHandlerConfig getHandlerConfigFile() {
        //配置中心存储的配置
        String configJson = "{\"handler\":\"nullValueCheckHandler\",\"down\":false,\"next\":{\"handler\":\"priceCheckHandler\",\"next\":{\"handler\":\"stockCheckHandler\",\"next\":null}}}";
        //转成Config对象
        ProductCheckHandlerConfig handlerConfig = JSONUtil.toBean(configJson, ProductCheckHandlerConfig.class);
        return handlerConfig;
    }

    public static Result executeChain(AbstractCheckHandler handler, ProductVO param) {
        //执行处理器
        Result handlerResult = handler.handle(param);
        if (!handlerResult.isSuccess()) {
            System.out.println("HandlerClient 责任链执行失败返回：" + handlerResult.toString());
            return handlerResult;
        }
        return Result.success();
    }

    /**
     * 获取处理器
     *
     * @param config
     * @return
     */
    private AbstractCheckHandler getHandler(ProductCheckHandlerConfig config) {
        //配置检查：没有配置处理器链路，则不执行校验逻辑
        if (Objects.isNull(config)) {
            return null;
        }
        //配置错误
        String handler = config.getHandler();
        if (StrUtil.isBlank(handler)) {
            return null;
        }
        //配置了不存在的处理器
        AbstractCheckHandler abstractCheckHandler = handlerMap.get(config.getHandler());
        if (Objects.isNull(abstractCheckHandler)) {
            return null;
        }

        //处理器设置配置Config
        abstractCheckHandler.setConfig(config);

        //递归设置链路处理器
        abstractCheckHandler.setNextHandler(this.getHandler(config.getNext()));

        return abstractCheckHandler;
    }

}
