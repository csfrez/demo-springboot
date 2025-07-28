package com.csfrez.redis.lock;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Redis锁基础服务
 */
@Slf4j
public class BaseRedisLock {

    /**
     * 标识名称
     */
    private final String name;

    /**
     * 锁生成方式
     */
    private final UnaryOperator<String> lockGenerator;

    public BaseRedisLock(String name) {
        this(name, t -> t);
    }

    public BaseRedisLock(String name, UnaryOperator<String> lockGenerator) {
        this.name = name;
        this.lockGenerator = Objects.requireNonNull(lockGenerator, "lock generator must not be null");
    }

    private static final String LOCK_PREFIX = "redis:lock:";

    private static final String LOCK_START_LOG = "{} {} 锁执行开始";

    private static final String LOCK_END_LOG = "{} {} 锁执行结束";

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(30);

    private static final int DEFAULT_RETRIES = -1;

    private String genLock(String identity) {
        return LOCK_PREFIX + name.toLowerCase(Locale.ROOT) + ":" + lockGenerator.apply(identity);
    }

    private String lock(String identity, Duration lockTimeout, int retries) {
        String lock = genLock(identity);
        boolean lockResult = RedisDistributedLock.tryLock(lock, lockTimeout.getSeconds());
        if (!lockResult) {
            throw new RuntimeException("获取分布式锁失败");
        }
        log.info(LOCK_START_LOG, name, identity);
        return lock;
    }

    private void release(String identity, String lock) {
        log.info(LOCK_END_LOG, name, identity);
        RedisDistributedLock.release(lock);
    }

    public void consume(String identity, Runnable runnable) {
        consume(identity, runnable, DEFAULT_DURATION, DEFAULT_RETRIES);
    }

    public void consume(String identity, Runnable runnable, Duration lockTimeout) {
        consume(identity, runnable, lockTimeout, DEFAULT_RETRIES);
    }

    public void consume(String identity, Runnable runnable, Duration lockTimeout, int retries) {
        String lock = lock(identity, lockTimeout, retries);
        try {
            runnable.run();
        } finally {
            release(identity, lock);
        }
    }

    public void consume(String identity, Consumer<String> process) {
        consume(identity, process, DEFAULT_DURATION, DEFAULT_RETRIES);
    }

    public void consume(String identity, Consumer<String> process, Duration lockTimeout) {
        consume(identity, process, lockTimeout, DEFAULT_RETRIES);
    }

    public void consume(String identity, Consumer<String> process, Duration lockTimeout, int retries) {
        String lock = lock(identity, lockTimeout, retries);
        try {
            process.accept(identity);
        } finally {
            release(identity, lock);
        }
    }

    public <R> R execute(String identity, Function<String, R> process) {
        return execute(identity, process, DEFAULT_DURATION, DEFAULT_RETRIES);
    }

    public <R> R execute(String identity, Function<String, R> process, Duration lockTimeout) {
        return execute(identity, process, lockTimeout, DEFAULT_RETRIES);
    }

    public <R> R execute(String identity, Function<String, R> process, Duration lockTimeout, int retries) {
        String lock = lock(identity, lockTimeout, retries);
        R r;
        try {
            r = process.apply(identity);
        } finally {
            release(identity, lock);
        }
        return r;
    }

    public <R> R supply(String identity, Supplier<R> process) {
        return supply(identity, process, DEFAULT_DURATION, DEFAULT_RETRIES);
    }

    public <R> R supply(String identity, Supplier<R> process, Duration lockTimeout) {
        return supply(identity, process, lockTimeout, DEFAULT_RETRIES);
    }

    public <R> R supply(String identity, Supplier<R> process, Duration lockTimeout, int retries) {
        String lock = lock(identity, lockTimeout, retries);
        R r;
        try {
            r = process.get();
        } finally {
            release(identity, lock);
        }
        return r;
    }
}
