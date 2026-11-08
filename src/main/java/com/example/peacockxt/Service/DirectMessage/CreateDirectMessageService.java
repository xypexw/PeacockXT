package com.example.peacockxt.Service.DirectMessage;


import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.PrivateMessagePayload;
import com.example.peacockxt.Infrastructure.Cache.DirectMessageRedisDao;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.DirectMessage;
import com.example.peacockxt.Models.Event;
import com.example.peacockxt.Models.PrivateSession;
import com.example.peacockxt.Repository.Imlpementation.DirectMessageRepository;
import com.example.peacockxt.Service.Provider.PrivateSessionProvider;
import com.example.peacockxt.Service.Provider.UserProvider;
import com.example.peacockxt.Service.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CreateDirectMessageService {
    private final UserProvider userProvider;
    private final DirectMessageRedisDao directMessageRedisDao;
    private final DirectMessageRepository directMessageRepository;
    private final PrivateSessionProvider  privateSessionProvider;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private LocalEventDispatcher localEventDispatcher;

    private final String DEFAULT_STATUS = "DEFAULT";
    private final String event_name = "CREATE_PRIVATE_MESSAGE";


    @Transactional
    public void createDirectMessage(Long senderId , String content , Long replyToId , Long privateSessionId ){
        PrivateSession privateSession = privateSessionProvider.getPrivateSession(privateSessionId);
        if(!userProvider.hasUser(senderId)){
            throw new BusinessLogicException("user does not exist");
        }
        if(privateSession == null){
            throw new BusinessLogicException("user does not exist");
        }


        DirectMessage directMessage = directMessageBuilder(senderId,content,replyToId,privateSession);
        directMessageRepository.save(directMessage);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(directMessage,privateSessionId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                directMessageRedisDao.saveMessage(privateSessionId,directMessage);
            }
        });
    }

    private DirectMessage directMessageBuilder(Long senderId , String content , Long replyToId , PrivateSession privateSession ){
        return DirectMessage.builder()
                .messageId(snowflakeIdGenerator.nextId())
                .senderId(senderId)
                .content(content)
                .status(DEFAULT_STATUS)
                .createAt(LocalDateTime.now())
                .sendAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .replyToId(replyToId)
                .privateSession(privateSession)
                .build();
    }

    private InternalEvent<PrivateMessagePayload> internalEventBuilder(DirectMessage directMessage , Long privateSessionId ){
        PrivateMessagePayload privateMessagePayload = PrivateMessagePayload.builder()
                .messageId(directMessage.getMessageId())
                .senderId(directMessage.getSenderId())
                .content(directMessage.getContent())
                .status(DEFAULT_STATUS)
                .createAt(LocalDateTime.now())
                .sendAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .replyToId(directMessage.getReplyToId())
                .build();
        return InternalEvent.<PrivateMessagePayload>builder()
                .eventType(event_name)
                .payload(privateMessagePayload)
                .destination(WebsocketDestination.PRIVATE_SESSION.getDestination(privateSessionId))
                .build();
    }

}
