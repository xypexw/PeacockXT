package com.example.peacockxt.Service.Message;

import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.MessagePayload;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Models.Post;
import com.example.peacockxt.Policy.MessagePolicy;
import com.example.peacockxt.Repository.Imlpementation.MessageRepository;
import com.example.peacockxt.Infrastructure.Cache.MessageRedisDao;
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
public class EditMessageService {

    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;
    private final MessageProvider messageProvider;
    private final MessagePolicy messagePolicy;
    private final LocalEventDispatcher localEventDispatcher;
    private final String event_name = "UPDATE_MESSAGE";
    private final String EDITED_STATUS = "EDITED";

    @Transactional
    public void editMessage(Long messageId, String content, Long channelId) {
        Message message = messageProvider.getMessage(messageId);
        messagePolicy.validateCanEditMessage(channelId, message);
        message.setStatus(EDITED_STATUS);
        message.setUpdateAt(LocalDateTime.now());
        message.setContent(content);
        messageRepository.save(message);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(message,channelId));

        log.debug("Message persisted to DB: messageId={}", message.getMessageId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageRedisDao.deleteMessageData(message.getMessageId());
            }
        });
    }

    InternalEvent<MessagePayload> internalEventBuilder(Message message , long channelId){
           MessagePayload messagePayload = MessagePayload.builder()
                   .messageId(message.getMessageId())
                   .status(message.getStatus())
                   .updateAt(message.getUpdateAt())
                   .content(message.getContent())
                   .channelId(channelId)
                   .build();
           return InternalEvent.<MessagePayload>builder()
                   .eventType(event_name)
                   .payload(messagePayload)
                   .destination(WebsocketDestination.CHANNEL.getDestination(channelId))
                   .build();
    }

}