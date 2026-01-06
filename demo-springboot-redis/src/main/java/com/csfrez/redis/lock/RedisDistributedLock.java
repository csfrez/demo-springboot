package com.csfrez.redis.lock;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.Collections;

/**
 * redis 分布式锁
 */
@Slf4j
public class RedisDistributedLock {

    private static final StringRedisTemplate redisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
//    private static final ResourceScriptSource lockLuaSource = new ResourceScriptSource(new ClassPathResource("lua/lock.lua"));
//    private static final DefaultRedisScript<Boolean> lockScript = new DefaultRedisScript<>();
//    private static final ResourceScriptSource releaseLuaSource = new ResourceScriptSource(new ClassPathResource("lua/release.lua"));
//    private static final DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>();

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;


    /**
     * 用于存储当前线程的锁标识(即锁value)，防止锁被别人误删除。
     * 并且用于判断是否是当前线程的锁，以实现可重入。
     * ThreadLocal 的数据要及时清理，因为线程是从线程池中获取，会被复用
     */
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

//    static {
//        lockScript.setResultType(Boolean.class);
//        lockScript.setScriptSource(lockLuaSource);
//        releaseScript.setResultType(Long.class);
//        releaseScript.setScriptSource(releaseLuaSource);
//    }


    /**
     * 获取锁
     *
     * @param lockKey       锁的 key
     * @param expireSeconds 过期时间（秒）
     * @param maxRetries    最大重试次数
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, long expireSeconds, int maxRetries) {
        String lockValue = IdUtil.fastSimpleUUID();
        int sum = 0;
        for (int i = 0; i < maxRetries; i++) {
            try {
//                Boolean success = redisTemplate.execute(lockScript,
//                        Collections.singletonList(lockKey),
//                        lockValue, String.valueOf(expireSeconds));
                boolean success = tryLock(lockKey, lockValue, expireSeconds);
                if (success) {
                    threadLocal.set(lockValue);
                    return true;
                }
            } catch (Exception e) {
                log.error("尝试获取锁异常", e);
            }
            try {
                int times = i + 1;
                log.warn("尝试获取锁失败，正在第 {} 次重试", times);
                sum += times;
                long waitTime = 1000 * (sum);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey    锁的 key
     * @param requestId  请求唯一标识（用于释放锁时校验）
     * @param expireTime 锁的过期时间（毫秒）
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, String requestId, long expireTime) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            Object nativeConnection = connection.getNativeConnection();
            String result;
            if (nativeConnection instanceof Jedis) {
                result = ((Jedis) nativeConnection).set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            } else if (nativeConnection instanceof JedisCluster) {
                result = ((JedisCluster) nativeConnection).set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            } else {
                throw new IllegalStateException("Unsupported Redis connection: " + nativeConnection.getClass());
            }
            return LOCK_SUCCESS.equals(result);
        });
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的 key
     * @return 是否释放成功
     */
    public static void release(String lockKey) {
        String lockValue = threadLocal.get();
        if (StrUtil.isBlank(lockValue))
            return;

//        redisTemplate.execute(releaseScript,
//                Collections.singletonList(lockKey),
//                lockValue);
        releaseLock(lockKey, lockValue);

        threadLocal.remove();
    }

    /**
     * 释放分布式锁（Lua 脚本保证原子性）
     *
     * @param lockKey   锁的 key
     * @param requestId 请求唯一标识（必须与加锁时一致）
     * @return 是否释放成功
     */
    public static boolean releaseLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        Long result = redisTemplate.execute((RedisCallback<Long>) connection -> {
            Object nativeConnection = connection.getNativeConnection();
            if (nativeConnection instanceof Jedis) {
                return (Long) ((Jedis) nativeConnection).eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            } else if (nativeConnection instanceof JedisCluster) {
                return (Long) ((JedisCluster) nativeConnection).eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            } else {
                throw new IllegalStateException("Unsupported Redis connection: " + nativeConnection.getClass());
            }
        });
        return RELEASE_SUCCESS.equals(result);
    }

}