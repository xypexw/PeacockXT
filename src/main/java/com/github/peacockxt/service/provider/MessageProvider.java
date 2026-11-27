package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.MessageRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheMessage;
import com.github.peacockxt.models.Message;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.MessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class MessageProvider {

    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;

    public CacheMessage getMessage(Long messageId) {
        if (messageId == null) return null;
        CacheMessage cached = messageRedisDao.getMessageData(messageId);
        if (cached != null) {
            return cached;
        }

        try {
            Message entity = messageRepository.getMessagesByMessageId(messageId);
            if (entity == null) {
                return null;
            }
            CacheMessage cacheMessage = CacheMessage.builder()
                    .messageId(entity.getMessageId())
                    .content(entity.getContent())
                    .senderId(entity.getSenderId())
                    .createAt(entity.getCreateAt())
                    .updateAt(entity.getUpdateAt())
                    .status(entity.getStatus())
                    .replyToId(entity.getReplyToId())
                    .channelId(entity.getChannelId())
                    .build();
            messageRedisDao.saveMessagesBatch(Collections.singletonList(cacheMessage));
            return cacheMessage;
        } catch (Exception e) {
            throw new DatabaseException("Cannot fetch message from database", e);
        }
    }
}
