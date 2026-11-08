package com.example.peacockxt.Service.DirectMessage;


import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.PrivateMessagePayload;
import com.example.peacockxt.Infrastructure.Cache.DirectMessageRedisDao;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.DirectMessage;
import com.example.peacockxt.Models.PrivateSession;
import com.example.peacockxt.Repository.Imlpementation.DirectMessageRepository;
import com.example.peacockxt.Service.Provider.DirectMessageProvider;
import com.example.peacockxt.Service.Provider.PrivateSessionProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class DeleteDirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageRedisDao  directMessageRedisDao;
    private final DirectMessageProvider directMessageProvider;
    private final PrivateSessionProvider  privateSessionProvider;
    private final String DELETED_STATUS = "DELETED";
    private final String event_name = "DELETED_PRIVATE_MESSAGE";
    private final String DELETED_CONTENT = "Message have been deleted";
    private final LocalEventDispatcher localEventDispatcher;

    @Transactional
    public void deletedDirectMessage( Long directMessageId , Long privateSessionId , Long currentUserId ) {
        PrivateSession privateSession = privateSessionProvider.getPrivateSession(privateSessionId);
        if(privateSession == null) {
            throw new BusinessLogicException("session not found");
        }

        boolean isParticipant = privateSession.getConnectorId().equals(currentUserId)
                || privateSession.getReceiverId().equals(currentUserId);

        if (!isParticipant) {
            throw new BusinessLogicException("user do not belong to this session");
        }

        DirectMessage message = directMessageProvider.getMessage(directMessageId);

        if(message == null) {
            throw new BusinessLogicException("message not found");
        }

        message.setStatus(DELETED_STATUS);
        message.setContent(DELETED_CONTENT);
        message.setUpdatedAt(LocalDateTime.now());

        directMessageRepository.save(message);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(message,privateSessionId ));

        TransactionSynchronizationManager.registerSynchronization( new TransactionSynchronization () {
            @Override
            public void afterCommit() {
                directMessageRedisDao.deleteMessageData(directMessageId);
            }
        });

    }

    InternalEvent<PrivateMessagePayload> internalEventBuilder(DirectMessage directMessage , Long privateSessionId ) {
        PrivateMessagePayload payload = PrivateMessagePayload.builder()
                .messageId(directMessage.getMessageId())
                .content(directMessage.getContent())
                .updatedAt(directMessage.getUpdatedAt())
                .status(DELETED_STATUS)
                .build();
        return InternalEvent.<PrivateMessagePayload>builder()
                .eventType(event_name)
                .payload(payload)
                .destination(WebsocketDestination.PRIVATE_SESSION.getDestination(privateSessionId))
                .build();
    }

}
