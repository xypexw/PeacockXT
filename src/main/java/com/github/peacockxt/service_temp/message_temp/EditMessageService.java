package com.github.peacockxt.service_temp.message_temp;

import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.dto_temp.event_temp.MessagePayload;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.Message;
import com.github.peacockxt.policy_temp.MessagePolicy;
import com.github.peacockxt.repository_temp.implementation.MessageRepository;
import com.github.peacockxt.infrastructure_temp.cache_temp.MessageRedisDao;
import com.github.peacockxt.service_temp.provider_temp.MessageProvider;
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