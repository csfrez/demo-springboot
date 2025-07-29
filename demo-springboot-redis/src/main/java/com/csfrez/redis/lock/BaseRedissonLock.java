package com.csfrez.redis.lock;

import cn.hutool.extra.spring.SpringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 基于redisson的分布式锁封装
 */
@Slf4j
public class BaseRedissonLock {

    /**
     * 标识名称
     */
    private final String name;

    /**
     * 锁生成方式
     */
    private final UnaryOperator<String> lockGenerator;

    /**
     * redissonClient
     */
    private final static RedissonClient redissonClient = SpringUtil.getBean(RedissonClient.class);

    public BaseRedissonLock(String name) {
        this(name, t -> t);
    }

    public BaseRedissonLock(String name, UnaryOperator<String> lockGenerator) {
        this.name = name;
        this.lockGenerator = Objects.requireNonNull(lockGenerator, "lock generator must not be null");
    }

    // redisson:lock:{name}:{identity}
    private static final String LOCK_PREFIX = "redisson:lock:";

    private static final String LOCK_START_LOG = "{} {} 锁执行开始";

    private static final String LOCK_END_LOG = "{} {} 锁执行结束";

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(30);

    private String genLockKey(String identity) {
        return LOCK_PREFIX + name.toLowerCase(Locale.ROOT) + ":" + lockGenerator.apply(identity);
    }

    protected RLock lock(String identity, Duration lockTimeout) {
        RLock rLock = redissonClient.getLock(genLockKey(identity));
        if (Objects.nonNull(lockTimeout)) {
            rLock.lock(lockTimeout.getSeconds(), TimeUnit.SECONDS);
        } else {
            rLock.lock();
        }
        log.info(LOCK_START_LOG, name, identity);
        return rLock;
    }

    protected RLock lock(String identity) {
        RLock rLock = redissonClient.getLock(genLockKey(identity));
        rLock.lock(DEFAULT_DURATION.getSeconds(), TimeUnit.SECONDS);
        log.info(LOCK_START_LOG, name, identity);
        return rLock;
    }

    protected void release(String identity, RLock rLock) {
        log.info(LOCK_END_LOG, name, identity);
        if (rLock.isHeldByCurrentThread() && rLock.isLocked()) {
            rLock.unlock();
        } else {
            log.warn("{} {} 锁异常提前释，可能存在超时", name, identity);
        }
    }

    public void consume(String identity, Runnable runnable) {
        RLock lock = lock(identity);
        try {
            runnable.run();
        } finally {
            release(identity, lock);
        }
    }

//    public void consume(String identity, Consumer<String> process) {
//        RLock lock = lock(identity);
//        try {
//            process.accept(identity);
//        } finally {
//            release(identity, lock);
//        }
//    }

    public <R> R execute(String identity, Function<String, R> process) {
        RLock lock = lock(identity);
        R r;
        try {
            r = process.apply(identity);
        } finally {
            release(identity, lock);
        }
        return r;
    }

    public <R> R supply(String identity, Supplier<R> process) {
        RLock lock = lock(identity);
        R r;
        try {
            r = process.get();
        } finally {
            release(identity, lock);
        }
        return r;
    }
}
