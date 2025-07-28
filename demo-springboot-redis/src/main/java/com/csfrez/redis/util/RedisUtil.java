package com.csfrez.redis.util;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * @author
 * @date 2025/7/28 9:34
 * @email
 */
public class RedisUtil {

    private static RedisTemplate<String, Object> myRedisTemplate = SpringUtil.getBean(RedisTemplate.class);

    public static <T> T executeLua(RedisScript<T> script, List<String> keys, Object... args) {
        return myRedisTemplate.execute(script, keys, args);
    }
}
