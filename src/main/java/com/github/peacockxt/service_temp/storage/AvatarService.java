package com.github.peacockxt.service_temp.storage;
import com.github.peacockxt.infrastructure_temp.cache_temp.UserSessionRedisDao;
import com.github.peacockxt.models_temp.customexception_temp.BusinessLogicException;
import com.github.peacockxt.models_temp.User;
import com.github.peacockxt.repository_temp.implementation.UserRepository;
import com.github.peacockxt.service_temp.provider_temp.UserProvider;
import com.github.peacockxt.service_temp.RedisRetryManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@AllArgsConstructor
@Slf4j
public class AvatarService {

    private final UserRepository userRepository;
    private final UserSessionRedisDao userSessionRedisDao;
    private final UserProvider userProvider;
    private final RedisRetryManager  redisRetryManager;
    @Transactional
    public void updateAvatarLink(String objectName, Long userId) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }
        User user = userProvider.getUser(userId);
        user.setAvatarUrl(objectName);
        userRepository.save(user);


        executeAfterCommit(() -> userSessionRedisDao.deleteUserProfile(userId));
    }

    @Transactional
    public void deleteAvatarLink(Long userId) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }
        User user = userProvider.getUser(userId);
        user.setAvatarUrl("");
        userRepository.save(user);

        executeAfterCommit(() -> userSessionRedisDao.deleteUserProfile(userId));
    }

    private void executeAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("Failed to execute task after transaction commit: {}", e.getMessage(), e);
                        redisRetryManager.execute(task);
                    }
                }
            });
        } else {
            redisRetryManager.execute(task);
        }
    }
}