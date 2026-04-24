package com.spendwisebackend.spendwisebackend.config.JWT;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    
    public void addToBlacklist(String token, long expirationMs) {
        redisTemplate.opsForValue().set(
            "blacklist:" + token, "1", 
            Duration.ofMillis(expirationMs));
    }
    
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}

