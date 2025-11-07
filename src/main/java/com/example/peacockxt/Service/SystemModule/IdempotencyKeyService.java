package com.example.peacockxt.Service.SystemModule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class IdempotencyKeyService {

    private final StringRedisTemplate stringRedisTemplate;

    public IdempotencyKeyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    private final String keyPrefix = "IdempotencyKey::";

    public boolean insertingKey(String key){
         final String state = "Active";
         boolean insertState = Boolean.TRUE.equals(
                 stringRedisTemplate.opsForValue().setIfAbsent(keyPrefix + key, state, 100, TimeUnit.SECONDS));
         if(insertState){
             return true;
         }
         else throw new RuntimeException("Redis have problem");
    }

    public void deletingKey(String key){
        stringRedisTemplate.opsForValue().getAndDelete(keyPrefix + key);
    }

    public boolean checkKey(String key){
        return stringRedisTemplate.hasKey(keyPrefix + key);
    }

}

