package com.github.peacockxt.service.directmessage;

import com.github.peacockxt.dto.response.DirectMessageResponse;
import com.github.peacockxt.infrastructure.cache.DirectMessageRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheDirectMessage;
import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.models.DirectMessage;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.DirectMessageRepository;
import com.github.peacockxt.service.provider.PrivateSessionProvider;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ReadDirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageRedisDao directMessageRedisDao;
    private final PrivateSessionProvider privateSessionProvider;
    private final UserProvider userProvider;
    private final int cacheLimit = 300;
    private final int defaultLimit = 50;
    private final Pageable pageable = PageRequest.of(0, defaultLimit);

    public List<DirectMessageResponse> readDirectMessage(Long sessionId, Long cursor, Long userId) {
        CachePrivateSession privateSession = privateSessionProvider.getPrivateSession(sessionId);
        if (privateSession == null) {
            throw new BusinessLogicException("Session Not Found");
        }
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User Not Found");
        }
        boolean isParticipant = userId.equals(privateSession.getConnectorId())
                || userId.equals(privateSession.getReceiverId());
        if (!isParticipant) {
            throw new BusinessLogicException("User does not belong to this session");
        }

        List<Long> indexes = getDirectMessageIndex(sessionId, cursor);
        if (indexes.isEmpty()) {
            return Collections.emptyList();
        }
        List<CacheDirectMessage> directMessages = getDirectMessages(indexes);
        Map<Long, CacheDirectMessage> replyMap = replyMessageMapBuilder(directMessages);
        return directMessageResponseListBuilder(directMessages, replyMap);
    }

    private List<Long> getDirectMessageIndex(Long sessionId, Long cursorId) {
        Set<Long> messageIds = directMessageRedisDao.getMessageIdsBeforeCursor(sessionId, cursorId, defaultLimit);
        if (messageIds == null || messageIds.isEmpty()) {
            if (cursorId == null) {
                Pageable pageableForCacheMiss = PageRequest.of(0, cacheLimit);
                List<Long> indexes = directMessageRepository.getMessageIdsByCursorId(
                        sessionId, Long.MAX_VALUE, pageableForCacheMiss
                );
                if (indexes == null || indexes.isEmpty()) {
                    return Collections.emptyList();
                }
                directMessageRedisDao.saveMessageIndexes(sessionId, indexes);
                return indexes.subList(0, Math.min(defaultLimit, indexes.size()));
            } else {
                List<Long> indexes = directMessageRepository.getMessageIdsByCursorId(
                        sessionId, cursorId, pageable
                );
                return (indexes == null) ? Collections.emptyList() : indexes;
            }
        }
        return new ArrayList<>(messageIds);
    }

    private List<CacheDirectMessage> getDirectMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheDirectMessage> cached = directMessageRedisDao.getMessagesBatch(messageIds);
        Map<Long, CacheDirectMessage> dmPool = new HashMap<>(messageIds.size());
        if (cached != null) {
            for (CacheDirectMessage dm : cached) {
                if (dm != null) {
                    dmPool.put(dm.getMessageId(), dm);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : messageIds) {
            if (!dmPool.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<DirectMessage> fromDb = directMessageRepository.getDirectMessagesByMessageIdIn(missingIds);
            List<CacheDirectMessage> toCache = new ArrayList<>(fromDb.size());
            for (DirectMessage entity : fromDb) {
                CacheDirectMessage cacheDm = toCacheDirectMessage(entity);
                dmPool.put(cacheDm.getMessageId(), cacheDm);
                toCache.add(cacheDm);
            }
            if (!toCache.isEmpty()) {
                directMessageRedisDao.multiSetMessageData(toCache);
            }
        }

        List<CacheDirectMessage> result = new ArrayList<>(messageIds.size());
        for (Long id : messageIds) {
            CacheDirectMessage dm = dmPool.get(id);
            if (dm != null) {
                result.add(dm);
            }
        }
        return result;
    }

    Map<Long, CacheDirectMessage> replyMessageMapBuilder(List<CacheDirectMessage> directMessageList) {
        Set<Long> replyIdSet = new LinkedHashSet<>();
        for (CacheDirectMessage directMessage : directMessageList) {
            if (directMessage != null && directMessage.getReplyToId() != null) {
                replyIdSet.add(directMessage.getReplyToId());
            }
        }
        if (replyIdSet.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CacheDirectMessage> replyMessages = getDirectMessages(new ArrayList<>(replyIdSet));
        Map<Long, CacheDirectMessage> result = new HashMap<>(replyMessages.size());
        for (CacheDirectMessage directMessage : replyMessages) {
            result.put(directMessage.getMessageId(), directMessage);
        }
        return result;
    }

    List<DirectMessageResponse> directMessageResponseListBuilder(List<CacheDirectMessage> directMessages,
                                                                 Map<Long, CacheDirectMessage> replyMessages) {
        List<DirectMessageResponse> result = new ArrayList<>();
        for (CacheDirectMessage directMessage : directMessages) {
            if (directMessage == null) continue;
            DirectMessageResponse directMessageResponse = new DirectMessageResponse();
            directMessageResponse.setMessageId(directMessage.getMessageId().toString());
            if (directMessage.getSenderId() != null) {
                directMessageResponse.setSenderId(directMessage.getSenderId().toString());
            }
            directMessageResponse.setStatus(directMessage.getStatus());
            directMessageResponse.setContent(directMessage.getContent());
            if (directMessage.getReplyToId() != null && replyMessages.containsKey(directMessage.getReplyToId())) {
                CacheDirectMessage replyMessage = replyMessages.get(directMessage.getReplyToId());
                directMessageResponse.setReplyToId(directMessage.getReplyToId().toString());
                directMessageResponse.setReplyContent(replyMessage.getContent());
                if (replyMessage.getSenderId() != null) {
                    directMessageResponse.setReplierId(replyMessage.getSenderId().toString());
                }
            }
            result.add(directMessageResponse);
        }
        return result;
    }

    private CacheDirectMessage toCacheDirectMessage(DirectMessage entity) {
        return CacheDirectMessage.builder()
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
    }
}
