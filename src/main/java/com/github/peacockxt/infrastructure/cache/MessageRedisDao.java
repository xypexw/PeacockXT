package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheMessage;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageRedisDao {

    private static final int MAX_INDEX_SIZE = 300;

    private static final DefaultRedisScript<Long> ADD_IF_EXISTS_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
            "  redis.call('ZADD', KEYS[1], 0, ARGV[1]); " +
            "  redis.call('ZREMRANGEBYRANK', KEYS[1], 0, tonumber(ARGV[2])); " +
            "  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3])); " +
            "  return 1; " +
            "end; " +
            "return 0;",
            Long.class
    );

    private final RedisTemplate<String, CacheMessage> messageRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    private static String formatId(Long id) {
        return String.format("%019d", id);
    }

    public void saveMessage(Long channelId, CacheMessage message) {
        if (channelId == null || message == null || message.getMessageId() == null) return;

        redisGuard.safeWrite(() -> {
            Long messageId = message.getMessageId();

            messageRedisTemplate.opsForValue().set(
                    RedisKey.MESSAGE_DATA.build(messageId),
                    message,
                    addJitter(RedisKey.MESSAGE_DATA.getTtl())
            );

            String indexKey = RedisKey.CHANNEL_MSG_INDEX.build(channelId);
            Duration indexTtl = addJitter(RedisKey.CHANNEL_MSG_INDEX.getTtl());
            stringRedisTemplate.execute(
                    ADD_IF_EXISTS_SCRIPT,
                    Collections.singletonList(indexKey),
                    formatId(messageId),
                    String.valueOf(-(MAX_INDEX_SIZE + 1)),
                    String.valueOf(indexTtl.getSeconds())
            );
        }, "saveMessage [MessageId: " + message.getMessageId() + "] for Channel: " + channelId);
    }

    public CacheMessage getMessageData(Long messageId) {
        if (messageId == null) return null;
        return redisGuard.safeRead(
                () -> messageRedisTemplate.opsForValue().get(RedisKey.MESSAGE_DATA.build(messageId)),
                null,
                "getMessageData for MessageId: " + messageId
        );
    }

    public List<CacheMessage> getMessagesBatch(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = messageIds.stream()
                    .map(RedisKey.MESSAGE_DATA::build)
                    .collect(Collectors.toList());

            List<CacheMessage> messages = messageRedisTemplate.opsForValue().multiGet(keys);
            return messages == null ? Collections.emptyList() : messages;
        }, Collections.emptyList(), "getMessagesBatch for " + messageIds.size() + " messages");
    }

    public Boolean hasMessageInfo(Long messageId) {
        if (messageId == null) return false;
        return redisGuard.safeRead(
                () -> messageRedisTemplate.hasKey(RedisKey.MESSAGE_DATA.build(messageId)),
                false,
                "hasMessageInfo for MessageId: " + messageId
        );
    }

    public Set<Long> getMessageIdsBeforeCursor(Long channelId, Long cursorMessageId, int limit) {
        if (channelId == null || limit <= 0) return Collections.emptySet();

        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.CHANNEL_MSG_INDEX.build(channelId);
            Set<String> raw;
            if (cursorMessageId == null) {
                raw = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
            } else {
                raw = stringRedisTemplate.opsForZSet().reverseRangeByLex(
                        indexKey,
                        Range.leftUnbounded(Range.Bound.exclusive(formatId(cursorMessageId))),
                        Limit.limit().count(limit)
                );
            }
            if (raw == null || raw.isEmpty()) return Collections.emptySet();
            Set<Long> result = new LinkedHashSet<>(raw.size());
            for (String s : raw) {
                result.add(Long.parseLong(s));
            }
            return result;
        }, Collections.emptySet(), "getMessageIdsBeforeCursor for Channel: " + channelId);
    }

    public void deleteMessageData(Long messageId) {
        if (messageId == null) return;
        redisGuard.safeInvalidate(
                () -> messageRedisTemplate.delete(RedisKey.MESSAGE_DATA.build(messageId)),
                "deleteMessageData for MessageId: " + messageId
        );
    }

    public void deleteChannelMsgIndex(Long channelId) {
        if (channelId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.delete(RedisKey.CHANNEL_MSG_INDEX.build(channelId)),
                "deleteChannelMsgIndex for Channel: " + channelId
        );
    }

    public void saveMessagesBatch(List<CacheMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, CacheMessage> messageMap = new HashMap<>();
            for (CacheMessage message : messages) {
                if (message != null && message.getMessageId() != null) {
                    messageMap.put(RedisKey.MESSAGE_DATA.build(message.getMessageId()), message);
                }
            }
            redisBatch.multiSetWithTtl(
                    messageRedisTemplate,
                    messageMap,
                    addJitter(RedisKey.MESSAGE_DATA.getTtl())
            );
        }, "saveMessagesBatch for " + messages.size() + " messages");
    }

    public void saveMessageIdsBatch(Long channelId, List<Long> messageIds) {
        if (channelId == null || messageIds == null || messageIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.CHANNEL_MSG_INDEX.build(channelId);
            Set<TypedTuple<String>> tuples = new HashSet<>(messageIds.size());
            for (Long messageId : messageIds) {
                if (messageId != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(messageId), 0.0));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
                stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(key, addJitter(RedisKey.CHANNEL_MSG_INDEX.getTtl()));
            }
        }, "saveMessageIdsBatch for Channel: " + channelId);
    }
}
