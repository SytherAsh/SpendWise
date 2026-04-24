package com.spendwisebackend.spendwisebackend.config.Security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    
    // Limits per minute
    private static final int CAPACITY = 60;
    private static final int LOGIN_CAPACITY = 10;
    private static final String REDIS_PREFIX = "rate_limit:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String ip = getClientIP(request);
        String path = request.getRequestURI();
        
        boolean isLoginRequest = path.contains("/auth/login");
        int limit = isLoginRequest ? LOGIN_CAPACITY : CAPACITY;
        String type = isLoginRequest ? "login" : "general";
        
        String key = REDIS_PREFIX + type + ":" + ip;
        
        if (isAllowed(key, limit)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"too many requset. try agin after 3 minute\"}");
        }
    }

    private boolean isAllowed(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count != null && count <= limit;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
