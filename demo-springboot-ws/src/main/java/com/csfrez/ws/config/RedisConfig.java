package com.csfrez.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

/**
 * @author
 * @date 2025/6/19 15:59
 * @email
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, List<String>> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, List<String>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * 默认是JDK的序列化策略，这里配置redisTemplate采用的是Jackson2JsonRedisSerializer的序列化策略
     * @param factory
     * @return
     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
//        Jackson2JsonRedisSerializer<Object> jacksonSeial = new Jackson2JsonRedisSerializer<>(Object.class);
//
//        // 使用Jackson序列号对象
//        ObjectMapper objectMapper = new ObjectMapper();
//        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会抛出异常
//        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        jacksonSeial.setObjectMapper(objectMapper);
//
//        // 使用RedisTemplate对象
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        // 配置连接工厂
//        template.setConnectionFactory(factory);
//        // 使用StringRedisSerializer来序列化和反序列化redis的key值
//        template.setKeySerializer(new StringRedisSerializer());
//        // 值采用json序列化
//        template.setValueSerializer(jacksonSeial);
//        // 使用StringRedisSerializer来序列化和反序列化redis的hash-key值
//        template.setHashKeySerializer(new StringRedisSerializer());
//        // 值采用json序列化
//        template.setHashValueSerializer(jacksonSeial);
//
//        //执行后续方法
//        template.afterPropertiesSet();
//        return template;
//    }
}