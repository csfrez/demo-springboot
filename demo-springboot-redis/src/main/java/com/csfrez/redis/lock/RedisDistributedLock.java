package com.csfrez.redis.lock;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;

/**
 * redis 分布式锁
 */
@Slf4j
public class RedisDistributedLock {

    private static final StringRedisTemplate redisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
    private static final ResourceScriptSource lockLuaSource = new ResourceScriptSource(new ClassPathResource("lua/lock.lua"));
    private static final DefaultRedisScript<Boolean> lockScript = new DefaultRedisScript<>();
    private static final ResourceScriptSource releaseLuaSource = new ResourceScriptSource(new ClassPathResource("lua/release.lua"));
    private static final DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>();

    /**
     * 用于存储当前线程的锁标识(即锁value)，防止锁被别人误删除。
     * 并且用于判断是否是当前线程的锁，以实现可重入。
     * ThreadLocal 的数据要及时清理，因为线程是从线程池中获取，会被复用
     */
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    static {
        lockScript.setResultType(Boolean.class);
        lockScript.setScriptSource(lockLuaSource);
        releaseScript.setResultType(Long.class);
        releaseScript.setScriptSource(releaseLuaSource);
    }

    /**
     * 获取锁
     *
     * @param lockKey       锁的 key
     * @param expireSeconds 过期时间（秒）
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, long expireSeconds) {
        String lockValue = IdUtil.fastSimpleUUID();
        Boolean success = redisTemplate.execute(lockScript,
                Collections.singletonList(lockKey),
                lockValue, String.valueOf(expireSeconds));
        if (Boolean.TRUE.equals(success)) {
            threadLocal.set(lockValue);
            return true;
        }
        return false;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的 key
     * @return 是否释放成功
     */
    public static void release(String lockKey) {
        String lockValue = threadLocal.get();
        if (StrUtil.isBlank(lockValue)) return;

        redisTemplate.execute(releaseScript,
                Collections.singletonList(lockKey),
                lockValue);

        threadLocal.remove();
    }

}