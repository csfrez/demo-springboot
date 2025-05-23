package com.csfrez.retry.rest;

import com.csfrez.retry.service.RetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author
 * @date 2025/5/23 16:22
 * @email
 */
@RestController
@RequestMapping("/retry")
public class RetryController {

    private final RetryService retryService;

    public RetryController(RetryService retryService) {
        this.retryService = retryService;
    }

    @GetMapping("/network")
    public String network() {
        return retryService.retryNetworkCall();
    }

    @GetMapping("/database")
    public String database() {
        return retryService.retryDatabaseCall();
    }
}