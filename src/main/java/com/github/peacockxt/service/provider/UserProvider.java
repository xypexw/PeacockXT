package com.github.peacockxt.service.provider;

import com.github.peacockxt.models.User;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.UserRepository;
import com.github.peacockxt.infrastructure.cache.UserSessionRedisDao;
import com.github.peacockxt.service.RedisRetryManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserProvider {

    private final UserRepository userRepository;
    private final UserSessionRedisDao userSessionRedisDao;
    private final RedisRetryManager redisRetryManager;

    public boolean hasUser(Long userId) {
        User userInCache = hasUserInCache(userId);
        if (userInCache != null) return true;

        try {
            User user = userRepository.getUserByUserId(userId);
            if (user == null) return false;
            saveUserProfileToCache(user);
            return true;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query user from database", e);
        }
    }

    public User getUser(Long userId) {
        User userInCache = hasUserInCache(userId);
        if (userInCache != null) return userInCache;
        try {
            return userRepository.getUserByUserId(userId);
        }
        catch (Exception e) {
            throw new DatabaseException("Failed to query user from database", e);
        }
    }

    private User hasUserInCache(Long userId) {
        try {
            return userSessionRedisDao.getUserProfile(userId);
        } catch (Exception e) {
            log.error("Failed to read user from Redis: {}", e.getMessage(), e);
            return null;
        }
    }

    private void saveUserProfileToCache(User user) {
        try {
            userSessionRedisDao.saveUserProfile(user.getUserId(), user);
        } catch (Exception e) {
            log.warn("Failed to cache user profile, queuing for retry: {}", e.getMessage());
            redisRetryManager.execute(() -> userSessionRedisDao.saveUserProfile(user.getUserId(), user));
        }
    }
}