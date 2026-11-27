package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.UserSessionRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheUser;
import com.github.peacockxt.models.User;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserProvider {

    private final UserRepository userRepository;
    private final UserSessionRedisDao userSessionRedisDao;

    public boolean hasUser(Long userId) {
        return getUser(userId) != null;
    }

    public CacheUser getUser(Long userId) {
        if (userId == null) return null;
        CacheUser userInCache = userSessionRedisDao.getUserProfile(userId);
        if (userInCache != null) return userInCache;

        try {
            User user = userRepository.getUserByUserId(userId);
            if (user == null) return null;

            CacheUser cacheUser = CacheUser.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .systemRole(user.getSystemRole())
                    .config(user.getConfig())
                    .status(user.getStatus())
                    .updateAt(user.getUpdateAt())
                    .createAt(user.getCreateAt())
                    .createBy(user.getCreateBy())
                    .updateBy(user.getUpdateBy())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .build();
            userSessionRedisDao.saveUserProfile(user.getUserId(), cacheUser);
            return cacheUser;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query user from database", e);
        }
    }
}
