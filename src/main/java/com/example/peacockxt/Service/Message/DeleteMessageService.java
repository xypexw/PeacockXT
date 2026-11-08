package com.example.peacockxt.Service.Message;

import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.MessagePayload;
import com.example.peacockxt.Infrastructure.Cache.MessageRedisDao;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Policy.MessagePolicy;
import com.example.peacockxt.Repository.Imlpementation.MessageRepository;
import com.example.peacockxt.Service.Provider.MessageProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class DeleteMessageService {

    private static final String STATUS_DELETED = "DELETED";
    private static final String DELETED_CONTENT = "Message has been deleted";
    private static final String EVENT_NAME = "DELETE_MESSAGE";
    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;
    private final MessageProvider messageProvider;
    private final MessagePolicy messagePolicy;

    private final LocalEventDispatcher localEventDispatcher;

    @Transactional
    public void deleteMessage(Long channelId, Long messageId) {
        Message message = messageProvider.getMessage(messageId);

        messagePolicy.validateCanDeleteMessage(channelId, message);

        message.setStatus(STATUS_DELETED);
        message.setUpdateAt(LocalDateTime.now());
        message.setContent(DELETED_CONTENT);

        messageRepository.save(message);
        log.debug("Message persisted to DB as deleted: messageId={}", message.getMessageId());

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(message, channelId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageRedisDao.deleteMessageData(message.getMessageId());
            }
        });
    }

    private InternalEvent<MessagePayload> internalEventBuilder(Message message, Long channelId) {
        MessagePayload payload = MessagePayload.builder()
                .messageId(message.getMessageId())
                .channelId(channelId)
                .status(message.getStatus())   // "DELETED"
                .content(message.getContent()) // "Message has been deleted"
                .build();

        return InternalEvent.<MessagePayload>builder()
                .eventType(EVENT_NAME)
                .payload(payload)
                .destination(WebsocketDestination.CHANNEL.getDestination(channelId))
                .build();
    }
}