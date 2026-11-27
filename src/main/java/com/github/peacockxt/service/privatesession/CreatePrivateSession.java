package com.github.peacockxt.service.privatesession;

import com.github.peacockxt.infrastructure.cache.PrivateSessionRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.models.PrivateSession;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.PrivateSessionRepository;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CreatePrivateSession {

    private final PrivateSessionRepository privateSessionRepository;
    private final PrivateSessionRedisDao privateSessionRedisDao;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final String DEFAULT_STATUS = "DEFAULT";
    private final UserProvider userProvider;

    @Transactional
    public void createPrivateSession(Long receiverId, Long connectorId) {

        if (!userProvider.hasUser(receiverId)) {
            throw new BusinessLogicException("Receiver does not exist");
        }
        if (!userProvider.hasUser(connectorId)) {
            throw new BusinessLogicException("connector does not exist");
        }

        Long smallerId = Math.min(receiverId, connectorId);
        Long largerId = Math.max(receiverId, connectorId);

        PrivateSession privateSession = privateSessionBuilder(smallerId, largerId, receiverId);
        privateSessionRepository.save(privateSession);

        CachePrivateSession cacheSession = CachePrivateSession.builder()
                .id(privateSession.getId())
                .connectorId(privateSession.getConnectorId())
                .receiverId(privateSession.getReceiverId())
                .createAt(privateSession.getCreateAt())
                .status(privateSession.getStatus())
                .createBy(privateSession.getCreateBy())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                privateSessionRedisDao.addSessionToReceiverIdAndConnectorId(
                        receiverId,
                        connectorId,
                        privateSession.getId()
                );
                privateSessionRedisDao.saveSessionInfo(privateSession.getId(), cacheSession);
            }
        });
    }

    private PrivateSession privateSessionBuilder(Long connectorId, Long receiverId, Long createBy) {
        return PrivateSession.builder()
                .id(snowflakeIdGenerator.nextId())
                .connectorId(connectorId)
                .receiverId(receiverId)
                .status(DEFAULT_STATUS)
                .createBy(createBy)
                .createAt(LocalDateTime.now())
                .build();
    }
}
