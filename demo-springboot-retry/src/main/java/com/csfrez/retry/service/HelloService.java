package com.csfrez.retry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/23 15:03
 * @email
 */
@Service
public class HelloService {

    @Autowired
    private RetryService retryService;

    public String hello() {
        return retryService.retryableMethod();
    }
}
