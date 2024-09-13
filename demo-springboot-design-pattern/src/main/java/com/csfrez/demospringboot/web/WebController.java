package com.csfrez.demospringboot.web;


import com.csfrez.demospringboot.chain.client.HandlerClient;
import com.csfrez.demospringboot.chain.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
public class WebController {

    @Autowired
    private HandlerClient handlerClient;

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring, Time is " + System.currentTimeMillis();
    }

    @GetMapping("/test")
    public String test() {
        //创建商品参数
        ProductVO param = ProductVO.builder()
                .skuId(null).skuName("华为手机").Path("http://...")
                .price(new BigDecimal(1))
                .stock(1)
                .build();
        log.info("param:{}", param);
        return handlerClient.paramCheck(param);
    }
}
