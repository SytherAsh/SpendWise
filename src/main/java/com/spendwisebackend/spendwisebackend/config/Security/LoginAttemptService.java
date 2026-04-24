package com.spendwisebackend.spendwisebackend.config.Security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;
    
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MINUTES = 15;

    public void loginSucceeded(String key) {
        redisTemplate.delete(getCacheKey(key));
    }

    public void loginFailed(String key) {
        String cacheKey = getCacheKey(key);
        Long attempts = redisTemplate.opsForValue().increment(cacheKey);
        
        if (attempts != null && attempts == 1) {

            redisTemplate.expire(cacheKey, Duration.ofMinutes(LOCK_TIME_DURATION_MINUTES));
        }
    }

    public boolean isBlocked(String key) {
        String attemptsStr = redisTemplate.opsForValue().get(getCacheKey(key));
        if (attemptsStr == null) {
            return false;
        }
        
        try {
            int attempts = Integer.parseInt(attemptsStr);
            return attempts >= MAX_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String getCacheKey(String key) {
        return "login_attempt:" + key;
    }
}
