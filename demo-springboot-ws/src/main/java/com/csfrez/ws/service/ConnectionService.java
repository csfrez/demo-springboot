package com.csfrez.ws.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.yeauty.pojo.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author
 * @date 2025/5/30 14:51
 * @email
 */
@Service
public class ConnectionService {

    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    private final Map<String, String> userIdMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, List<String>> redisTemplate;

    private static final String USER_ID_KEY = "userId:";

    public void addSession(String sessionId, Session session) {
        sessionMap.put(sessionId, session);
    }

    public void removeSession(String sessionId) {
        sessionMap.remove(sessionId);
    }

    public Session getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public void addUserId(String sessionId, String userId) {
        userIdMap.put(sessionId, userId);
    }

    public void removeUserId(String sessionId) {
        userIdMap.remove(sessionId);
    }

    public String getUserId(String sessionId) {
        return userIdMap.get(sessionId);
    }

    public void addSessionId(String userId, String sessionId) {
        String key = USER_ID_KEY + userId;
        List<String> sessionIdList = redisTemplate.opsForValue().get(key);
        if (CollUtil.isEmpty(sessionIdList)) {
            sessionIdList = new CopyOnWriteArrayList<>();
        }
        sessionIdList.add(sessionId);
        redisTemplate.opsForValue().set(key, sessionIdList);
    }

    public void removeSessionId(String userId, String sessionId) {
        String key = USER_ID_KEY + userId;
        List<String> sessionIdList = redisTemplate.opsForValue().get(key);
        if (CollUtil.isEmpty(sessionIdList)) {
            return;
        }
        sessionIdList.remove(sessionId);
        redisTemplate.opsForValue().set(key, sessionIdList);
    }

    public List<String> getSessionIdList(String userId) {
        String key = USER_ID_KEY + userId;
        return redisTemplate.opsForValue().get(key);
    }

    public Pair<Map<String, Session>, Map<String, String>> getAll() {
        return new Pair<>(sessionMap, userIdMap);
    }

    public void openSession(String sessionId, String userId, Session session) {
        this.addSession(sessionId, session);
        this.addUserId(sessionId, userId);
        this.addSessionId(userId, sessionId);
    }

    public void closeSession(String sessionId) {
        String userId = this.getUserId(sessionId);
        if (StrUtil.isNotBlank(userId)) {
            this.removeSessionId(userId, sessionId);
        }
        this.removeUserId(sessionId);
        this.removeSession(sessionId);
    }

    public Map<String, String> getUserIdMap() {
        return userIdMap;
    }

    public Set<String> getUserIdSet() {
        return CollUtil.newHashSet(userIdMap.values());
    }

    public void deleteUserId(String userId) {
        String key = USER_ID_KEY + userId;
        redisTemplate.delete(key);
    }

}