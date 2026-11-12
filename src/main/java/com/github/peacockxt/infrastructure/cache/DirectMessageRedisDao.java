package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.DirectMessage;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DirectMessageRedisDao {

    // Retain up to the 300 most recent direct messages per session
    private static final int MAX_INDEX_SIZE = 300;

    private final RedisTemplate<String, DirectMessage> dmRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    // --- HELPER: Add Jitter to TTL (Random from 0 to 30 minutes) ---
    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    // ==========================================
    // 1. SAVE / UPDATE MESSAGE (WRITE -> safeWrite)
    // ==========================================
    public void saveMessage(Long sessionId, DirectMessage message) {
        redisGuard.safeWrite(() -> {
            Long messageId = message.getMessageId();

            // 1. Save Message Data
            String dataKey = RedisKey.DIRECT_MESSAGE_DATA.build(messageId);
            dmRedisTemplate.opsForValue().set(
                    dataKey,
                    message,
                    addJitter(RedisKey.DIRECT_MESSAGE_DATA.getTtl())
            );

            // 2. Save Index to Session ZSet
            String indexKey = RedisKey.SESSION_MSG_INDEX.build(sessionId);
            longRedisTemplate.opsForZSet().add(indexKey, messageId, messageId.doubleValue());

            // 3. Trim Index to keep only the latest 300 messages
            longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
        }, "saveMessage: sessionId=" + sessionId + ", messageId=" + message.getMessageId());
    }

    public void saveMessageIndexes(Long sessionId, List<Long> messageIds) {
        if (sessionId == null || messageIds == null || messageIds.isEmpty()) {
            return;
        }

        redisGuard.safeWrite(() -> {
            String key = RedisKey.SESSION_MSG_INDEX.build(sessionId);
            Set<ZSetOperations.TypedTuple<Long>> tuples = new HashSet<>();
            for (Long messageId : messageIds) {
                tuples.add(new DefaultTypedTuple<>(messageId, messageId.doubleValue()));
            }
            longRedisTemplate.opsForZSet().add(key, tuples);
        }, "saveMessageIndexes: sessionId=" + sessionId + ", size=" + messageIds.size());
    }

    public void multiSetMessageData(List<DirectMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, DirectMessage> maps = new HashMap<>();
            for (DirectMessage message : messages) {
                maps.put(RedisKey.DIRECT_MESSAGE_DATA.build(message.getMessageId()), message);
            }

            redisBatch.multiSetWithTtl(
                    dmRedisTemplate,
                    maps,
                    addJitter(RedisKey.DIRECT_MESSAGE_DATA.getTtl())
            );
        }, "multiSetMessageData: size=" + messages.size());
    }

    // ==========================================
    // 2. GET MESSAGE - SINGLE & BATCH (READ -> safeRead)
    // ==========================================
    public DirectMessage getMessageData(Long messageId) {
        return redisGuard.safeRead(
                () -> dmRedisTemplate.opsForValue().get(RedisKey.DIRECT_MESSAGE_DATA.build(messageId)),
                null,
                "getMessageData: messageId=" + messageId
        );
    }

    public List<DirectMessage> getMessagesBatch(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = messageIds.stream()
                    .map(RedisKey.DIRECT_MESSAGE_DATA::build)
                    .collect(Collectors.toList());

            List<DirectMessage> messages = dmRedisTemplate.opsForValue().multiGet(keys);
            return (messages == null || messages.isEmpty()) ? Collections.emptyList() : messages;
        }, Collections.emptyList(), "getMessagesBatch: size=" + messageIds.size());
    }

    // ==========================================
    // 3. CURSOR - PAGINATION (READ -> safeRead)
    // ==========================================
    public Set<Long> getMessageIdsBeforeCursor(Long sessionId, Long cursorMessageId, int limit) {
        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.SESSION_MSG_INDEX.build(sessionId);

            if (cursorMessageId == null) {
                // Initial page load: retrieve the most recent message IDs
                Set<Long> result = longRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
                return result == null ? Collections.emptySet() : result;
            } else {
                // Subsequent page loads: fetch messages older than cursor
                Set<Long> result = longRedisTemplate.opsForZSet().reverseRangeByScore(
                        indexKey, 0, cursorMessageId - 1, 0, limit
                );
                return result == null ? Collections.emptySet() : result;
            }
        }, Collections.emptySet(), "getMessageIdsBeforeCursor: sessionId=" + sessionId + ", cursor=" + cursorMessageId);
    }

    // ==========================================
    // 4. DELETE MESSAGE (WRITE/DELETE -> safeWrite)
    // ==========================================
    public void deleteMessageData(Long messageId) {
        redisGuard.safeWrite(
                () -> dmRedisTemplate.delete(RedisKey.DIRECT_MESSAGE_DATA.build(messageId)),
                "deleteMessageData: messageId=" + messageId
        );
    }
}