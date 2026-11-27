package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.DirectMessageRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheDirectMessage;
import com.github.peacockxt.models.DirectMessage;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.DirectMessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class DirectMessageProvider {
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageRedisDao directMessageRedisDao;

    public CacheDirectMessage getMessage(Long messageId) {
        if (messageId == null) return null;
        CacheDirectMessage cached = directMessageRedisDao.getMessageData(messageId);
        if (cached != null) {
            return cached;
        }

        try {
            DirectMessage entity = directMessageRepository.getDirectMessageByMessageId(messageId);
            if (entity == null) {
                return null;
            }
            CacheDirectMessage cacheDm = CacheDirectMessage.builder()
                    .messageId(entity.getMessageId())
                    .content(entity.getContent())
                    .senderId(entity.getSenderId())
                    .status(entity.getStatus())
                    .updatedAt(entity.getUpdatedAt())
                    .createAt(entity.getCreateAt())
                    .sendAt(entity.getSendAt())
                    .replyToId(entity.getReplyToId())
                    .sessionId(entity.getSessionId())
                    .build();
            directMessageRedisDao.multiSetMessageData(Collections.singletonList(cacheDm));
            return cacheDm;
        } catch (Exception e) {
            throw new DatabaseException("Cannot fetch message from database", e);
        }
    }
}
