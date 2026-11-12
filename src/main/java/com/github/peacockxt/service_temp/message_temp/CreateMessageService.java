package com.github.peacockxt.service_temp.message_temp;

import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.dto_temp.event_temp.MessagePayload; // DTO bạn cần tạo (giống PostPayload)
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.Channel;
import com.github.peacockxt.models_temp.Message;
import com.github.peacockxt.policy_temp.MessagePolicy;
import com.github.peacockxt.repository_temp.implementation.ChannelRepository;
import com.github.peacockxt.repository_temp.implementation.MessageRepository;
import com.github.peacockxt.infrastructure_temp.cache_temp.MessageRedisDao;
import com.github.peacockxt.service_temp.SnowflakeIdGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class CreateMessageService {

    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;
    private final ChannelRepository channelRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final MessagePolicy messagePolicy;

    private final LocalEventDispatcher localEventDispatcher;
    private static final String DEFAULT_STATUS = "DEFAULT";
    private static final String event_name = "CREATE_MESSAGE";

    @Transactional
    public void createMessage(Long senderId, String content, Long replyId, Long channelId, Long membershipId) {
        messagePolicy.validateCanCreateMessage(channelId, membershipId, senderId);

        Message message = messageBuilder(senderId, content, replyId, channelId);
        messageRepository.save(message);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(message, channelId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageRedisDao.saveMessage(channelId, message);
            }
        });
    }

    private Message messageBuilder(Long senderId, String content, Long replyId, Long channelId) {
        Channel channel = channelRepository.getReferenceById(channelId);
        return Message.builder()
                .messageId(snowflakeIdGenerator.nextId())
                .content(content)
                .createAt(LocalDateTime.now())
                .replyToId(replyId)
                .status(DEFAULT_STATUS)
                .updateAt(LocalDateTime.now())
                .senderId(senderId)
                .channel(channel)
                .build();
    }

    private InternalEvent<MessagePayload> internalEventBuilder(Message message, Long channelId) {
        MessagePayload payload = MessagePayload.builder()
                .messageId(message.getMessageId())
                .content(message.getContent())
                .createAt(message.getCreateAt())
                .replyToId(message.getReplyToId())
                .status(message.getStatus())
                .updateAt(message.getUpdateAt())
                .senderId(message.getSenderId())
                .channelId(channelId)
                .build();

        return InternalEvent.<MessagePayload>builder()
                .payload(payload)
                .eventType(event_name)
                .destination(WebsocketDestination.CHANNEL.getDestination(channelId))
                .build();
    }
}