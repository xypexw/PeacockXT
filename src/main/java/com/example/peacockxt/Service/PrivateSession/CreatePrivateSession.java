package com.example.peacockxt.Service.PrivateSession;

import com.example.peacockxt.Infrastructure.Cache.PrivateSessionRedisDao;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.PrivateSession;
import com.example.peacockxt.Repository.Imlpementation.PrivateSessionRepository;
import com.example.peacockxt.Service.Provider.UserProvider;
import com.example.peacockxt.Service.SnowflakeIdGenerator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CreatePrivateSession {

    private final PrivateSessionRepository privateSessionRepository;
    private final PrivateSessionRedisDao privateSessionRedisDao;
    private final SnowflakeIdGenerator  snowflakeIdGenerator;
    private final String DEFAULT_STATUS = "DEFAULT";
    private final UserProvider userProvider;

    @Transactional
    public void createPrivateSession( Long receiverId , Long connectorId ) {

        if(!userProvider.hasUser(receiverId)){
            throw new BusinessLogicException("Receiver does not exist");
        }
        if(!userProvider.hasUser(connectorId)){
            throw new BusinessLogicException("connector does not exist");
        }

        Long smallerId = Math.min(receiverId,connectorId);
        Long largerId = Math.max(receiverId,connectorId);


        PrivateSession privateSession = privateSessionBuilder(smallerId, largerId,receiverId);
        privateSessionRepository.save(privateSession);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        privateSessionRedisDao.addSessionToReceiverIdAndConnectorId(receiverId,
                                connectorId,
                                privateSession.getId());
                        privateSessionRedisDao.saveSessionInfo(privateSession.getId(),privateSession);
                    }
                }
        );

    }

    private PrivateSession privateSessionBuilder( Long connectorId , Long receiverId , Long createBy){
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
