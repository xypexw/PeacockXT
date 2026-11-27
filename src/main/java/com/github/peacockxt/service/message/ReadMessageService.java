package com.github.peacockxt.service.message;

import com.github.peacockxt.dto.response.MessageResponse;
import com.github.peacockxt.infrastructure.cache.MessageRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheMessage;
import com.github.peacockxt.models.Message;
import com.github.peacockxt.policy.MessagePolicy;
import com.github.peacockxt.repository.implementation.MessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ReadMessageService {

    private final MessageRepository messageRepository;
    private final MessageRedisDao messageRedisDao;
    private final MessagePolicy messagePolicy;

    private final int limitIndexPageForCacheMiss = 300;
    private final Pageable limitIndexProvider = PageRequest.of(0, limitIndexPageForCacheMiss);
    private final int limitIndexAndValue = 30;
    private final Pageable limitPageProvider = PageRequest.of(0, limitIndexAndValue);

    public List<MessageResponse> readMessages(Long channelId, Long cursorId, Long membershipId) {
        messagePolicy.validateCanReadMessage(channelId, membershipId);
        List<Long> messageIndex = getMessageIndex(channelId, cursorId);
        if (messageIndex.isEmpty()) {
            return Collections.emptyList();
        }
        List<CacheMessage> messages = getMessages(channelId, messageIndex);
        Map<Long, CacheMessage> replyMessages = findReplyMessage(channelId, messages);

        return responsesBuilder(messages, replyMessages);
    }

    private List<Long> getMessageIndex(Long channelId, Long cursorId) {
        Set<Long> messageIds = messageRedisDao.getMessageIdsBeforeCursor(channelId, cursorId, limitIndexAndValue);
        if (messageIds != null && !messageIds.isEmpty()) {
            return new ArrayList<>(messageIds);
        }

        if (cursorId == null) {
            List<Long> indexes = messageRepository.getMessageIdsByCursorId(channelId, Long.MAX_VALUE, limitIndexProvider);
            if (indexes == null || indexes.isEmpty()) {
                return Collections.emptyList();
            }
            messageRedisDao.saveMessageIdsBatch(channelId, indexes);
            return indexes.subList(0, Math.min(limitIndexAndValue, indexes.size()));
        } else {
            List<Long> indexes = messageRepository.getMessageIdsByCursorId(channelId, cursorId, limitPageProvider);
            return (indexes == null) ? Collections.emptyList() : indexes;
        }
    }

    private List<CacheMessage> getMessages(Long channelId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheMessage> cached = messageRedisDao.getMessagesBatch(messageIds);
        Map<Long, CacheMessage> messagePool = new HashMap<>(messageIds.size());
        if (cached != null) {
            for (CacheMessage m : cached) {
                if (m != null) {
                    messagePool.put(m.getMessageId(), m);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : messageIds) {
            if (!messagePool.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Message> fromDb = messageRepository.getMessagesByListOfMessageId(channelId, missingIds);
            List<CacheMessage> toCache = new ArrayList<>(fromDb.size());
            for (Message entity : fromDb) {
                CacheMessage cacheMessage = toCacheMessage(entity);
                messagePool.put(cacheMessage.getMessageId(), cacheMessage);
                toCache.add(cacheMessage);
            }
            if (!toCache.isEmpty()) {
                messageRedisDao.saveMessagesBatch(toCache);
            }
        }

        List<CacheMessage> resultList = new ArrayList<>(messageIds.size());
        for (Long id : messageIds) {
            CacheMessage msg = messagePool.get(id);
            if (msg != null) {
                resultList.add(msg);
            }
        }
        return resultList;
    }

    private Map<Long, CacheMessage> findReplyMessage(Long channelId, List<CacheMessage> messages) {
        Set<Long> replyIdsSet = new LinkedHashSet<>();
        for (CacheMessage message : messages) {
            if (message != null && message.getReplyToId() != null) {
                replyIdsSet.add(message.getReplyToId());
            }
        }

        if (replyIdsSet.isEmpty()) return Collections.emptyMap();

        List<CacheMessage> replyMessages = getMessages(channelId, new ArrayList<>(replyIdsSet));
        Map<Long, CacheMessage> replyMessageMap = new HashMap<>(replyMessages.size());
        for (CacheMessage message : replyMessages) {
            replyMessageMap.put(message.getMessageId(), message);
        }
        return replyMessageMap;
    }

    private List<MessageResponse> responsesBuilder(List<CacheMessage> messages, Map<Long, CacheMessage> replyMessageMap) {
        List<MessageResponse> result = new ArrayList<>();
        for (CacheMessage message : messages) {
            MessageResponse temporary = new MessageResponse();
            temporary.setMessageId(message.getMessageId().toString());
            temporary.setContent(message.getContent());
            temporary.setCreateAt(message.getCreateAt());
            temporary.setStatus(message.getStatus());
            if (message.getSenderId() != null) {
                temporary.setSenderId(message.getSenderId().toString());
            }

            if (message.getReplyToId() != null) {
                CacheMessage reply = replyMessageMap.get(message.getReplyToId());
                if (reply != null) {
                    temporary.setReplyToId(reply.getMessageId().toString());
                    temporary.setReplyContent(reply.getContent());
                    if (reply.getSenderId() != null) {
                        temporary.setReplierId(reply.getSenderId().toString());
                    }
                }
            }
            result.add(temporary);
        }
        return result;
    }

    private CacheMessage toCacheMessage(Message entity) {
        return CacheMessage.builder()
                .messageId(entity.getMessageId())
                .content(entity.getContent())
                .senderId(entity.getSenderId())
                .createAt(entity.getCreateAt())
                .updateAt(entity.getUpdateAt())
                .status(entity.getStatus())
                .replyToId(entity.getReplyToId())
                .channelId(entity.getChannelId())
                .build();
    }
}
