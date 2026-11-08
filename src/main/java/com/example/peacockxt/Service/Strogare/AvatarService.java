package com.example.peacockxt.Service.Strogare;
import com.example.peacockxt.Infrastructure.Cache.UserSessionRedisDao;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.User;
import com.example.peacockxt.Repository.Imlpementation.UserRepository;
import com.example.peacockxt.Service.Provider.UserProvider;
import com.example.peacockxt.Service.RedisRetryManager;
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