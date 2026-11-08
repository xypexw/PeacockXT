package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Infrastructure.Cache.DirectMessageRedisDao;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.DirectMessage;
import com.example.peacockxt.Repository.Imlpementation.DirectMessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class DirectMessageProvider {
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageRedisDao  directMessageRedisDao;

    public DirectMessage getMessage(Long messageId) {
        try {
            DirectMessage directMessage = directMessageRedisDao.getMessageData(messageId);
            if (directMessage != null) {
                return directMessage;
            }
        } catch (Exception e) {
            log.error("Redis error when getting message {}: {}", messageId, e.getMessage());
        }

        try {
            return directMessageRepository.getDirectMessageByMessageId(messageId);
        } catch (Exception e) {
            throw new DatabaseException("Cannot fetch message from database", e);
        }
    }

}
