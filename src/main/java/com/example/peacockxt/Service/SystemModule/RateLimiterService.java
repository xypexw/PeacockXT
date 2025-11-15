package com.example.peacockxt.Service.SystemModule;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
    private final RedisTemplate<String,Integer> redisTemplate;
    private final String rateLimiterKeyPrefix = "RATE_LIMITER::";
    private final int maxAccess = 100;
    private final int timeWindow = 60;
    public RateLimiterService(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

}
