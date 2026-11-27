package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheUser;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class UserSessionRedisDao {

    private final RedisTemplate<String, CacheUser> userRedisTemplate;
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
        if (username == null || userId == null) return;
        redisGuard.safeWrite(
                () -> userIdRedisTemplate.opsForValue().set(
                        RedisKey.USER_MAPPING_USERNAME.build(username),
                        userId,
                        addJitter(RedisKey.USER_MAPPING_USERNAME.getTtl())
                ),
                "mapUsernameToId: username=" + username + ", userId=" + userId
        );
    }

    public Long getIdByUsername(String username) {
        if (username == null) return null;
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
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> userRedisTemplate.hasKey(RedisKey.USER_PROFILE.build(userId)),
                false,
                "hasUserProfile: userId=" + userId
        ));
    }

    public void saveUserProfile(Long userId, CacheUser user) {
        if (userId == null || user == null) return;
        redisGuard.safeWrite(
                () -> userRedisTemplate.opsForValue().set(
                        RedisKey.USER_PROFILE.build(userId),
                        user,
                        addJitter(RedisKey.USER_PROFILE.getTtl())
                ),
                "saveUserProfile: userId=" + userId
        );
    }

    public CacheUser getUserProfile(Long userId) {
        if (userId == null) return null;
        return redisGuard.safeRead(
                () -> userRedisTemplate.opsForValue().get(RedisKey.USER_PROFILE.build(userId)),
                null,
                "getUserProfile: userId=" + userId
        );
    }

    public void deleteUserProfile(Long userId) {
        if (userId == null) return;
        redisGuard.safeInvalidate(
                () -> userRedisTemplate.delete(RedisKey.USER_PROFILE.build(userId)),
                "deleteUserProfile: userId=" + userId
        );
    }

    // ==========================================
    // 3. SESSION & REFRESH TOKEN (AUTH)
    // ==========================================

    public void saveSessionToken(long userId, String accessToken) {
        redisGuard.safeWrite(
                () -> stringRedisTemplate.opsForValue().set(
                        RedisKey.SESSION_TOKEN.build(accessToken),
                        String.valueOf(userId),
                        RedisKey.SESSION_TOKEN.getTtl()
                ),
                "saveSessionToken: userId=" + userId
        );
    }

    public void saveRefreshToken(Long userId, String refreshToken) {
        redisGuard.safeWrite(
                () -> stringRedisTemplate.opsForValue().set(
                        RedisKey.REFRESH_TOKEN.build(refreshToken),
                        String.valueOf(userId),
                        RedisKey.REFRESH_TOKEN.getTtl()
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
        if (userId == null) return;
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
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> stringRedisTemplate.hasKey(RedisKey.USER_ONLINE.build(userId)),
                false,
                "isUserOnline: userId=" + userId
        ));
    }
}
