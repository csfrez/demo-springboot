package com.csfrez.ws.servie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author
 * @date 2025/5/30 14:50
 * @email
 */
@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean validateToken(String token) {
//        return redisTemplate.opsForValue().get("token:" + token) != null;
//        return Boolean.TRUE.equals(redisTemplate.hasKey("token:" + token));
        return Boolean.TRUE;
    }
}