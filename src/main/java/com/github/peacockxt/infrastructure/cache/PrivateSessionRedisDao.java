package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class PrivateSessionRedisDao {

    private static final int MAX_INDEX_SIZE = 1000;

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

    private final RedisTemplate<String, CachePrivateSession> privateSessionRedisTemplate;
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

    public void saveSessionInfo(Long sessionId, CachePrivateSession session) {
        if (sessionId == null || session == null) return;
        redisGuard.safeWrite(
                () -> privateSessionRedisTemplate.opsForValue().set(
                        RedisKey.PRIVATE_SESSION_INFO.build(sessionId),
                        session,
                        addJitter(RedisKey.PRIVATE_SESSION_INFO.getTtl())
                ),
                "saveSessionInfo: sessionId=" + sessionId
        );
    }

    public void addSessionToReceiverIdAndConnectorId(Long receiverId, Long connectorId, Long sessionId) {
        if (receiverId == null || connectorId == null || sessionId == null) return;
        redisGuard.safeWrite(() -> {
            Duration indexTtl = addJitter(RedisKey.USER_INBOX_INDEX.getTtl());
            String formattedId = formatId(sessionId);
            String trimArg = String.valueOf(-(MAX_INDEX_SIZE + 1));
            String ttlArg = String.valueOf(indexTtl.getSeconds());

            stringRedisTemplate.execute(
                    ADD_IF_EXISTS_SCRIPT,
                    Collections.singletonList(RedisKey.USER_INBOX_INDEX.build(receiverId)),
                    formattedId, trimArg, ttlArg
            );
            if (!receiverId.equals(connectorId)) {
                stringRedisTemplate.execute(
                        ADD_IF_EXISTS_SCRIPT,
                        Collections.singletonList(RedisKey.USER_INBOX_INDEX.build(connectorId)),
                        formattedId, trimArg, ttlArg
                );
            }
        }, "addSessionToReceiverIdAndConnectorId: sessionId=" + sessionId);
    }

    public Set<Long> getAllSessionInbox(Long userId) {
        if (userId == null) return Collections.emptySet();
        return redisGuard.safeRead(() -> {
            Set<String> raw = stringRedisTemplate.opsForZSet().reverseRange(RedisKey.USER_INBOX_INDEX.build(userId), 0, -1);
            if (raw == null || raw.isEmpty()) return Collections.emptySet();
            Set<Long> result = new LinkedHashSet<>(raw.size());
            for (String s : raw) {
                result.add(Long.parseLong(s));
            }
            return result;
        }, Collections.emptySet(), "getAllSessionInbox: userId=" + userId);
    }

    public List<CachePrivateSession> getSessionBatch(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return Collections.emptyList();
        return redisGuard.safeRead(() -> {
            List<String> keys = new ArrayList<>(sessionIds.size());
            for (Long id : sessionIds) {
                keys.add(RedisKey.PRIVATE_SESSION_INFO.build(id));
            }
            List<CachePrivateSession> result = privateSessionRedisTemplate.opsForValue().multiGet(keys);
            return (result == null || result.isEmpty()) ? Collections.emptyList() : result;
        }, Collections.emptyList(), "getSessionBatch: size=" + sessionIds.size());
    }

    public void saveAllSessionIdsToInbox(Long userId, List<Long> sessionIds) {
        if (userId == null || sessionIds == null || sessionIds.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            String key = RedisKey.USER_INBOX_INDEX.build(userId);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(sessionIds.size());
            for (Long id : sessionIds) {
                if (id != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(id), 0.0));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
                stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(key, addJitter(RedisKey.USER_INBOX_INDEX.getTtl()));
            }
        }, "saveAllSessionIdsToInbox: userId=" + userId + ", size=" + sessionIds.size());
    }

    public void saveSessionInfoBatch(List<CachePrivateSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            Map<String, CachePrivateSession> map = new HashMap<>();
            for (CachePrivateSession privateSession : sessions) {
                if (privateSession != null && privateSession.getId() != null) {
                    map.put(RedisKey.PRIVATE_SESSION_INFO.build(privateSession.getId()), privateSession);
                }
            }
            Duration timeToLive = addJitter(RedisKey.PRIVATE_SESSION_INFO.getTtl());
            redisBatch.multiSetWithTtl(privateSessionRedisTemplate, map, timeToLive);
        }, "saveSessionInfoBatch: size=" + sessions.size());
    }

    public void deleteSessionData(Long sessionId) {
        if (sessionId == null) return;
        redisGuard.safeInvalidate(
                () -> privateSessionRedisTemplate.delete(RedisKey.PRIVATE_SESSION_INFO.build(sessionId)),
                "deleteSessionData: sessionId=" + sessionId
        );
    }
}
