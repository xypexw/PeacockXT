package com.github.peacockxt.service.directmessage;

import com.github.peacockxt.dto.response.DirectMessageResponse;
import com.github.peacockxt.infrastructure.cache.DirectMessageRedisDao;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.DirectMessage;
import com.github.peacockxt.models.PrivateSession;
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
    private final DirectMessageRedisDao  directMessageRedisDao;
    private final PrivateSessionProvider  privateSessionProvider;
    private final UserProvider userProvider;
    private final int cacheLimit = 300;
    private final int defaultLimit = 50;
    private final Pageable pageable = PageRequest.of(0,defaultLimit);


    public List<DirectMessageResponse> readDirectMessage(Long sessionId , Long cursor , Long userId ){
        PrivateSession privateSession = privateSessionProvider.getPrivateSession(sessionId);
        if(privateSession==null){
            throw new BusinessLogicException("Session Not Found");
        }
        if(!userProvider.hasUser(userId)){
            throw new BusinessLogicException("User Not Found");
        }

        List<Long> indexes = getDirectMessageIndex(sessionId,cursor);
        List<DirectMessage> directMessages = getDirectMessages(indexes);
        Map<Long,DirectMessage> replyMap = replyMessageMapBuilder(directMessages);
        return directMessageResponseListBuilder(directMessages,replyMap);
    }



    private List<Long> getDirectMessageIndex(Long sessionId, Long cursorId) {
        // 1. Get Data Form Cache
        Set<Long> messageIds = directMessageRedisDao.getMessageIdsBeforeCursor(sessionId, cursorId, defaultLimit);
        if (messageIds == null || messageIds.isEmpty()) {
            // 2. Cache Miss -> Fetch Database
            Long effectiveCursor ;
            List<Long> indexes = new ArrayList<>();
            if(cursorId == null){
                effectiveCursor = Long.MAX_VALUE;
                Pageable pageableForCacheMiss = PageRequest.of(0,cacheLimit);
                directMessageRepository.getMessageIdsByCursorId(sessionId, effectiveCursor,pageableForCacheMiss);
                directMessageRedisDao.saveMessageIndexes(sessionId,indexes);
            }
            else{
                effectiveCursor = cursorId;
                directMessageRepository.getMessageIdsByCursorId(sessionId,effectiveCursor,pageable);
            }
            return indexes.subList(0,Math.min(defaultLimit,indexes.size()));
        }
        return new ArrayList<>(messageIds);
    }

    private List<DirectMessage> getDirectMessages(List<Long> messageIds) {
        List<DirectMessage> directMessagesInCache = directMessageRedisDao.getMessagesBatch(messageIds);
        List<Long> missingIds = new ArrayList<>();
        for(int i = 0; i < directMessagesInCache.size(); i++){
            if(directMessagesInCache.get(i) == null){
                missingIds.add(messageIds.get(i));
            }
        }
        if(missingIds.isEmpty()){
            return directMessagesInCache;
        }

        List<DirectMessage> directMessagesInDatabase =
                directMessageRepository.getDirectMessagesByMessageIdIn(missingIds);
        Map<Long,DirectMessage> dmPool = new  HashMap<>();
        for(DirectMessage directMessage : directMessagesInDatabase){
            dmPool.put(directMessage.getMessageId(), directMessage);
        }
        List<DirectMessage> result = new ArrayList<>();
        for(int i = 0; i < messageIds.size(); i++){
            DirectMessage directMessage = directMessagesInCache.get(i);
            if(directMessage == null && dmPool.containsKey(messageIds.get(i))){
                directMessage = dmPool.get(messageIds.get(i));
            }

            if(directMessage != null){
                result.add(directMessage);
            }
        }
        return result;
    }

    Map<Long,DirectMessage> replyMessageMapBuilder(List<DirectMessage> directMessageList ){
        Map<Long,DirectMessage> result = new HashMap<>();
        List<Long> replyIds = new ArrayList<>();
        for(DirectMessage directMessage : directMessageList){
            if(directMessage.getReplyToId()!=null){
                replyIds.add(directMessage.getReplyToId());
            }
        }
        List<DirectMessage> replyMessages = getDirectMessages(replyIds);
        for(DirectMessage directMessage : replyMessages){
            result.put(directMessage.getMessageId(), directMessage);
        }
        return result;
    }

    List<DirectMessageResponse> directMessageResponseListBuilder(List<DirectMessage> directMessages ,
                                                                 Map<Long,DirectMessage> replyMessages){
        List<DirectMessageResponse> result = new ArrayList<>();
        for(DirectMessage directMessage : directMessages){
            if(directMessage == null) continue;
            DirectMessageResponse directMessageResponse = new DirectMessageResponse();
            directMessageResponse.setMessageId(directMessage.getMessageId().toString());
            directMessageResponse.setSenderId(directMessage.getSenderId().toString());
            directMessageResponse.setStatus(directMessage.getStatus());
            directMessageResponse.setContent(directMessage.getContent());
            if(directMessage.getReplyToId() != null && replyMessages.containsKey(directMessage.getReplyToId())){
                DirectMessage replyMessage = replyMessages.get(directMessage.getReplyToId());
                directMessageResponse.setReplyToId(directMessage.getReplyToId().toString());
                directMessageResponse.setReplyContent(replyMessage.getContent());
                directMessageResponse.setReplierId(replyMessage.getSenderId().toString());
            }
            result.add(directMessageResponse);
        }
        return result;
    }

}
