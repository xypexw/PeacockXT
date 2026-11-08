package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Repository.Imlpementation.MessageRepository;
import com.example.peacockxt.Infrastructure.Cache.MessageRedisDao;
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