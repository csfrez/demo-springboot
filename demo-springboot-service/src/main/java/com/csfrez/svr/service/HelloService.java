package com.csfrez.svr.service;

import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/23 15:03
 * @email
 */
@Service
public class HelloService {


    public String hello(String name) {
        return String.valueOf(System.currentTimeMillis()) + " hello " + name;
    }
}
