package com.example.peacockxt.Service.Message;

import com.example.peacockxt.DTO.Respone.MessageResponse;
import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Policy.MessagePolicy;
import com.example.peacockxt.Repository.Imlpementation.MessageRepository;
import com.example.peacockxt.Infrastructure.Cache.MessageRedisDao;
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

    public List<MessageResponse> readMessages(Long channelId, Long cursorId, Long membershipId) {
        messagePolicy.validateCanReadMessage(channelId, membershipId);
        List<Long> messageIndex = getMessageIndex(channelId, cursorId);
        List<Message> messages = getMessages(channelId, messageIndex);
        Map<Long, Message> replyMessages = findReplyMessage(channelId, messages);

        return responsesBuilder(messages, replyMessages);
    }

    private List<Long> getMessageIndex(Long channelId, Long cursorId) {
        // 1. Lấy từ Cache (RedisOperationGuard dưới DAO đã lo vụ hốt lỗi và trả về Set rỗng nếu chết)
        Set<Long> messageIds = messageRedisDao.getMessageIdsBeforeCursor(channelId, cursorId, limitIndexAndValue);

        if (messageIds == null || messageIds.isEmpty()) {
            // 2. Cache hụt -> Lội Database
            Long effectiveCursor = (cursorId != null) ? cursorId : Long.MAX_VALUE;
            List<Long> indexes = messageRepository.getMessageIdsByCursorId(channelId, effectiveCursor, limitIndexProvider);
            // Lưu lại index vào Redis
            messageRedisDao.saveMessageIdsBatch(channelId, indexes);
            return indexes.subList(0, Math.min(limitIndexAndValue, indexes.size()));
        }

        return new ArrayList<>(messageIds);
    }

    private List<Message> getMessages(Long channelId, List<Long> messageIds) {
        // 1. Quét Cache
        List<Message> messagesList = messageRedisDao.getMessagesBatch(messageIds);

        // Cache Miss toàn bộ -> Lội Database 100%
        if (messagesList == null || messagesList.isEmpty()) {
            return getMessagesInDatabase(channelId, messageIds);
        }

        Map<Long, Message> messagePool = new HashMap<>();
        List<Long> messageIdsNeedToFill = new ArrayList<>();

        // 2. Lọc ra các ID hụt (Partial Miss)
        for (int i = 0; i < messageIds.size(); i++) {
            if (messagesList.get(i) == null) {
                messageIdsNeedToFill.add(messageIds.get(i));
            }
        }

        // Cache Hit 100%
        if (messageIdsNeedToFill.isEmpty()) return messagesList;

        // 3. Kéo bù Data từ DB và nhét vào Map + Update Cache
        List<Message> missingMessages = getMessagesInDatabase(channelId, messageIdsNeedToFill);
        for (Message message : missingMessages) {
            messagePool.put(message.getMessageId(), message);
        }
        messageRedisDao.saveMessagesBatch(missingMessages);

        // 4. Lắp ráp kết quả
        List<Message> resultList = new ArrayList<>(messageIds.size());
        for (int i = 0; i < messageIds.size(); i++) {
            Message msg = messagesList.get(i);
            if (msg == null) {
                msg = messagePool.get(messageIds.get(i));
            }
            if (msg != null) resultList.add(msg);
        }
        return resultList;
    }

    private List<Message> getMessagesInDatabase(Long channelId, List<Long> messageIds) {
        //  Nếu DB chết, Spring Data sẽ tự ném DataAccessException ra ngoài.
        List<Message> result = messageRepository.getMessagesByListOfMessageId(channelId, messageIds);
        result.sort(Comparator.comparing(Message::getMessageId).reversed());
        return result;
    }

    private Map<Long, Message> findReplyMessage(Long channelId, List<Message> messages) {
        List<Long> replyIdsFromMessagesList = new ArrayList<>();
        for (Message message : messages) {
            if (message.getReplyToId() != null) {
                replyIdsFromMessagesList.add(message.getReplyToId());
            }
        }

        if (replyIdsFromMessagesList.isEmpty()) return Collections.emptyMap();

        List<Message> replyMessages = getMessages(channelId, replyIdsFromMessagesList);
        Map<Long, Message> replyMessageMap = new HashMap<>();
        for (Message message : replyMessages) {
            replyMessageMap.put(message.getMessageId(), message);
        }
        return replyMessageMap;
    }

    private List<MessageResponse> responsesBuilder(List<Message> messages, Map<Long, Message> replyMessageMap) {
        List<MessageResponse> result = new ArrayList<>();
        for (Message message : messages) {
            MessageResponse temporary = new MessageResponse();
            temporary.setMessageId(message.getMessageId().toString());
            temporary.setContent(message.getContent());
            temporary.setCreateAt(message.getCreateAt());
            temporary.setStatus(message.getStatus());
            temporary.setSenderId(message.getSenderId().toString());

            if (message.getReplyToId() != null) {
                Message reply = replyMessageMap.get(message.getReplyToId());
                if (reply != null) {
                    temporary.setReplyToId(reply.getMessageId().toString());
                    temporary.setReplyContent(reply.getContent());
                    temporary.setReplierId(reply.getSenderId().toString());
                }
            }
            result.add(temporary);
        }
        return result;
    }
}