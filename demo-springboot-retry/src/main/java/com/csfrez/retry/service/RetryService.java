package com.csfrez.retry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/23 11:50
 * @email
 */
@Slf4j
@Service
public class RetryService {

    // 最大重试5次，每次间隔5秒，2倍递增
    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String retryableMethod() {
        double random = Math.random();
        log.info("Random number: {}", random);
        if (random < 0.9) {
            throw new RuntimeException("Simulated failure");
        }
        return "Success " + random;
    }

    @Recover
    public String recover(Exception e) {
        log.error("Recovering from exception: {}", e.getMessage());
        return "Recovery: " + e.getMessage();
    }

    private int networkAttempt = 0;
    private int dbAttempt = 0;

    // 网络请求重试（指数退避 + 重试3次）
    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 500, multiplier = 2))
    public String retryNetworkCall() {
        networkAttempt++;
        log.info("Network attempt: {}", networkAttempt);
        if (networkAttempt < 2) {
            throw new NetworkException("Network error");
        }
        return "Network call succeeded";
    }

    // 数据库操作重试（固定延迟 + 重试2次）
    @Retryable(include = DataAccessException.class, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public String retryDatabaseCall() {
        dbAttempt++;
        log.info("Database attempt: {}", dbAttempt);
        if (dbAttempt < 2) {
            throw new DataAccessException("Database error");
        }
        return "Database call succeeded";
    }

    // 恢复方法（所有重试失败后执行）
    @Recover
    public String recover(NetworkException e) {
        return "Recovered from network error: " + e.getMessage();
    }

    @Recover
    public String recover(DataAccessException e) {
        return "Recovered from database error: " + e.getMessage();
    }
}

// 自定义异常类
class NetworkException extends RuntimeException {
    public NetworkException(String message) {
        super(message);
    }
}
// 自定义异常类
class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }
}
