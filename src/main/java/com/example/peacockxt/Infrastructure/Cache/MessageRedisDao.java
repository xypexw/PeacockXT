package com.example.peacockxt.Infrastructure.Cache;

import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Infrastructure.Constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageRedisDao {

    private static final int MAX_INDEX_SIZE = 300;

    private final RedisTemplate<String, Message> messageRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;

    // Inject the operation guard here
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void saveMessage(Long channelId, Message message) {
        redisGuard.safeWrite(() -> {
            Long messageId = message.getMessageId();

            messageRedisTemplate.opsForValue().set(
                    RedisKey.MESSAGE_DATA.build(messageId),
                    message,
                    addJitter(RedisKey.MESSAGE_DATA.getTtl())
            );

            String indexKey = RedisKey.CHANNEL_MSG_INDEX.build(channelId);
            longRedisTemplate.opsForZSet().add(indexKey, messageId, messageId.doubleValue());
            longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));

        }, "saveMessage [MessageId: " + message.getMessageId() + "] for Channel: " + channelId);
    }

    public Message getMessageData(Long messageId) {
        return redisGuard.safeRead(
                () -> messageRedisTemplate.opsForValue().get(RedisKey.MESSAGE_DATA.build(messageId)),
                null,
                "getMessageData for MessageId: " + messageId
        );
    }

    public List<Message> getMessagesBatch(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = messageIds.stream()
                    .map(RedisKey.MESSAGE_DATA::build)
                    .collect(Collectors.toList());

            List<Message> messages = messageRedisTemplate.opsForValue().multiGet(keys);

            // Handle null safely inside the Guard
            return messages == null ? Collections.emptyList() : messages;

        }, Collections.emptyList(), "getMessagesBatch for " + messageIds.size() + " messages");
    }

    public Boolean hasMessageInfo(Long messageId) {
        return redisGuard.safeRead(
                () -> messageRedisTemplate.hasKey(RedisKey.MESSAGE_DATA.build(messageId)),
                false, // Return false if Redis is down
                "hasMessageInfo for MessageId: " + messageId
        );
    }

    public Set<Long> getMessageIdsBeforeCursor(Long channelId, Long cursorMessageId, int limit) {
        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.CHANNEL_MSG_INDEX.build(channelId);

            if (cursorMessageId == null) {
                return longRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
            }

            return longRedisTemplate.opsForZSet().reverseRangeByScore(
                    indexKey, 0, cursorMessageId - 1, 0, limit
            );

        }, Collections.emptySet(), "getMessageIdsBeforeCursor for Channel: " + channelId);
    }

    public void deleteMessageData(Long messageId) {
        redisGuard.safeWrite(
                () -> messageRedisTemplate.delete(RedisKey.MESSAGE_DATA.build(messageId)),
                "deleteMessageData for MessageId: " + messageId
        );
    }

    public void saveMessagesBatch(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, Message> messageMap = new HashMap<>();
            for (Message message : messages) {
                messageMap.put(
                        RedisKey.MESSAGE_DATA.build(message.getMessageId()),
                        message
                );
            }

            redisBatch.multiSetWithTtl(
                    messageRedisTemplate,
                    messageMap,
                    addJitter(RedisKey.MESSAGE_DATA.getTtl())
            );

        }, "saveMessagesBatch for " + messages.size() + " messages");
    }

    public void saveMessageIdsBatch(Long channelId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.CHANNEL_MSG_INDEX.build(channelId);
            Set<TypedTuple<Long>> tuples = messageIds.stream()
                    .map(messageId -> new DefaultTypedTuple<>(messageId, messageId.doubleValue()))
                    .collect(Collectors.toSet());

            longRedisTemplate.opsForZSet().addIfAbsent(key, tuples);
            longRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));

        }, "saveMessageIdsBatch for Channel: " + channelId);
    }
}