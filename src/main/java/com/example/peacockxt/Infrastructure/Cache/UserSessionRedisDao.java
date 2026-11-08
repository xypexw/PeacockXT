package com.example.peacockxt.Infrastructure.Cache;

import com.example.peacockxt.Models.User;
import com.example.peacockxt.Infrastructure.Constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class UserSessionRedisDao {

    private final RedisTemplate<String, User> userRedisTemplate;
    private final RedisTemplate<String, Long> userIdRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    // ==========================================
    // 1. USERNAME - USER_ID MAPPING
    // ==========================================

    public void mapUsernameToId(String username, Long userId) {
        redisGuard.safeWrite(
                () -> userIdRedisTemplate.opsForValue().set(RedisKey.USER_MAPPING_USERNAME.build(username), userId),
                "mapUsernameToId: username=" + username + ", userId=" + userId
        );
    }

    public Long getIdByUsername(String username) {
        return redisGuard.safeRead(
                () -> userIdRedisTemplate.opsForValue().get(RedisKey.USER_MAPPING_USERNAME.build(username)),
                null,
                "getIdByUsername: username=" + username
        );
    }

    // ==========================================
    // 2. USER PROFILE CACHE
    // ==========================================

    public boolean hasUserProfile(Long userId) {
        return redisGuard.safeRead(
                () -> Boolean.TRUE.equals(userIdRedisTemplate.hasKey(RedisKey.USER_PROFILE.build(userId))),
                false,
                "hasUserProfile: userId=" + userId
        );
    }

    public void saveUserProfile(Long userId, User user) {
        redisGuard.safeWrite(
                () -> userRedisTemplate.opsForValue().set(
                        RedisKey.USER_PROFILE.build(userId),
                        user,
                        addJitter(RedisKey.USER_PROFILE.getTtl())
                ),
                "saveUserProfile: userId=" + userId
        );
    }

    public User getUserProfile(Long userId) {
        return redisGuard.safeRead(
                () -> userRedisTemplate.opsForValue().get(RedisKey.USER_PROFILE.build(userId)),
                null,
                "getUserProfile: userId=" + userId
        );
    }

    public void deleteUserProfile(Long userId) {
        redisGuard.safeWrite(
                () -> userRedisTemplate.delete(RedisKey.USER_PROFILE.build(userId)),
                "deleteUserProfile: userId=" + userId
        );
    }

    // ==========================================
    // 3. SESSION & REFRESH TOKEN (AUTH)
    // ==========================================

    public void saveSessionToken(long userId, String accessToken) {
        // APISIX dùng key: "peacockxt:session:token:" + token
        redisGuard.safeWrite(
                () -> stringRedisTemplate.opsForValue().set(
                        RedisKey.SESSION_TOKEN.build(accessToken),
                        String.valueOf(userId),
                        Duration.ofMinutes(15) // AT live 15 mins
                ),
                "saveSessionToken: userId=" + userId
        );
    }

    public void saveRefreshToken(Long userId, String refreshToken) {
        // APISIX dùng key: "peacockxt:session:refresh_token:" + token
        redisGuard.safeWrite(
                () -> stringRedisTemplate.opsForValue().set(
                        RedisKey.REFRESH_TOKEN.build(refreshToken),
                        String.valueOf(userId),
                        Duration.ofDays(7) // RT live 7 days
                ),
                "saveRefreshToken: userId=" + userId
        );
    }

    public String getUserIdByRefreshToken(String refreshToken) {
        return redisGuard.safeRead(
                () -> stringRedisTemplate.opsForValue().get(RedisKey.REFRESH_TOKEN.build(refreshToken)),
                null,
                "getUserIdByRefreshToken"
        );
    }

    public Boolean hasSessionToken(String tokenKey) {
        return redisGuard.safeRead(
                () -> stringRedisTemplate.hasKey(RedisKey.SESSION_TOKEN.build(tokenKey)),
                false,
                "hasSessionToken"
        );
    }

    // ==========================================
    // 4. USER PRESENCE (ONLINE STATUS)
    // ==========================================

    public void markUserOnline(Long userId) {
        redisGuard.safeWrite(
                () -> stringRedisTemplate.opsForValue().set(
                        RedisKey.USER_ONLINE.build(userId),
                        String.valueOf(System.currentTimeMillis()),
                        RedisKey.USER_ONLINE.getTtl()
                ),
                "markUserOnline: userId=" + userId
        );
    }

    public boolean isUserOnline(Long userId) {
        return redisGuard.safeRead(
                () -> Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKey.USER_ONLINE.build(userId))),
                false,
                "isUserOnline: userId=" + userId
        );
    }
}