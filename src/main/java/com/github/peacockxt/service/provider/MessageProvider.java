package com.github.peacockxt.service.provider;

import com.github.peacockxt.models.Message;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.MessageRepository;
import com.github.peacockxt.infrastructure.cache.MessageRedisDao;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class MessageProvider {

    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;

    public Message getMessage(Long messageId) {
        try {
            Message message = messageRedisDao.getMessageData(messageId);
            if (message != null) {
                return message;
            }
        } catch (Exception e) {
            log.error("Redis error when getting message {}: {}", messageId, e.getMessage());
        }

        try {
            return messageRepository.getMessagesByMessageId(messageId);
        } catch (Exception e) {
            throw new DatabaseException("Cannot fetch message from database", e);
        }
    }
}