package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheEvent;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventRedisDao {

    private static final int MAX_INDEX_SIZE = 500;

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

    private final RedisTemplate<String, CacheEvent> eventRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(1800);
        return baseTtl.plusSeconds(jitterSeconds);
    }

    private static String formatId(Long id) {
        return String.format("%019d", id);
    }

    public void saveEvent(Long teamId, CacheEvent event) {
        if (teamId == null || event == null || event.getEventId() == null) return;

        redisGuard.safeWrite(() -> {
            Long eventId = event.getEventId();

            String dataKey = RedisKey.EVENT_DATA.build(eventId);
            Duration ttl = addJitter(RedisKey.EVENT_DATA.getTtl());
            eventRedisTemplate.opsForValue().set(dataKey, event, ttl);

            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);
            Duration indexTtl = addJitter(RedisKey.TEAM_EVENT_INDEX.getTtl());
            stringRedisTemplate.execute(
                    ADD_IF_EXISTS_SCRIPT,
                    Collections.singletonList(indexKey),
                    formatId(eventId),
                    String.valueOf(-(MAX_INDEX_SIZE + 1)),
                    String.valueOf(indexTtl.getSeconds())
            );
        }, "saveEvent: teamId=" + teamId + ", eventId=" + event.getEventId());
    }

    public void saveEventBatch(Long teamId, List<CacheEvent> events) {
        if (events == null || events.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            Map<String, CacheEvent> eventMap = new HashMap<>();
            for (CacheEvent event : events) {
                if (event != null && event.getEventId() != null) {
                    eventMap.put(RedisKey.EVENT_DATA.build(event.getEventId()), event);
                }
            }
            redisBatch.multiSetWithTtl(eventRedisTemplate, eventMap, addJitter(RedisKey.EVENT_DATA.getTtl()));
        }, "saveEventBatch: teamId=" + teamId + ", size=" + events.size());
    }

    public void saveEventIdsBatch(Long teamId, List<Long> eventIds) {
        if (teamId == null || eventIds == null || eventIds.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(eventIds.size());
            for (Long eventId : eventIds) {
                if (eventId != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(eventId), 0.0));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(indexKey, tuples);
                stringRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(indexKey, addJitter(RedisKey.TEAM_EVENT_INDEX.getTtl()));
            }
        }, "saveEventIdsBatch: teamId=" + teamId + ", size=" + eventIds.size());
    }

    public CacheEvent getEventData(Long eventId) {
        if (eventId == null) return null;
        return redisGuard.safeRead(() -> {
            String dataKey = RedisKey.EVENT_DATA.build(eventId);
            return eventRedisTemplate.opsForValue().get(dataKey);
        }, null, "getEventData: eventId=" + eventId);
    }

    public List<CacheEvent> getEventsBatch(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyList();
        return redisGuard.safeRead(() -> {
            List<String> keys = eventIds.stream()
                    .map(RedisKey.EVENT_DATA::build)
                    .collect(Collectors.toList());

            List<CacheEvent> events = eventRedisTemplate.opsForValue().multiGet(keys);
            if (events == null || events.isEmpty()) return Collections.emptyList();
            return events;
        }, Collections.emptyList(), "getEventsBatch: size=" + eventIds.size());
    }

    public Set<Long> getEventIdsBeforeCursor(Long teamId, Long cursorEventId, int limit) {
        if (teamId == null || limit <= 0) return Collections.emptySet();
        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);
            Set<String> raw;
            if (cursorEventId == null) {
                raw = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
            } else {
                raw = stringRedisTemplate.opsForZSet().reverseRangeByLex(
                        indexKey,
                        Range.leftUnbounded(Range.Bound.exclusive(formatId(cursorEventId))),
                        Limit.limit().count(limit)
                );
            }
            if (raw == null || raw.isEmpty()) return Collections.emptySet();
            Set<Long> result = new LinkedHashSet<>(raw.size());
            for (String s : raw) {
                result.add(Long.parseLong(s));
            }
            return result;
        }, Collections.emptySet(), "getEventIdsBeforeCursor: teamId=" + teamId + ", cursor=" + cursorEventId);
    }

    public void deleteEvent(Long teamId, Long eventId) {
        if (teamId == null || eventId == null) return;
        redisGuard.safeInvalidate(() -> {
            eventRedisTemplate.delete(RedisKey.EVENT_DATA.build(eventId));
            stringRedisTemplate.opsForZSet().remove(RedisKey.TEAM_EVENT_INDEX.build(teamId), formatId(eventId));
            stringRedisTemplate.delete(RedisKey.EVENT_REGISTRATION_COUNT.build(eventId));
        }, "deleteEvent: teamId=" + teamId + ", eventId=" + eventId);
    }

    public void deleteTeamEventIndex(Long teamId) {
        if (teamId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.delete(RedisKey.TEAM_EVENT_INDEX.build(teamId)),
                "deleteTeamEventIndex: teamId=" + teamId
        );
    }

    public void incrementRegistrationCount(Long eventId) {
        String key = RedisKey.EVENT_REGISTRATION_COUNT.build(eventId);
        stringRedisTemplate.opsForValue().increment(key, 1);
        stringRedisTemplate.expire(key, addJitter(RedisKey.EVENT_REGISTRATION_COUNT.getTtl()));
    }

    public void deleteEventData(Long eventId) {
        if (eventId == null) return;
        redisGuard.safeInvalidate(
                () -> eventRedisTemplate.delete(RedisKey.EVENT_DATA.build(eventId)),
                "deleteEventData: eventId=" + eventId
        );
    }

    public Set<String> scanRegistrationCountKeys() {
        return redisGuard.safeRead(() -> {
            Set<String> keys = new LinkedHashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(RedisKey.EVENT_REGISTRATION_COUNT.build("*"))
                    .count(200)
                    .build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            return keys;
        }, Collections.emptySet(), "scanRegistrationCountKeys");
    }

    public Long consumeRegistrationCount(String key) {
        if (key == null || key.isBlank()) return null;
        return redisGuard.safeRead(() -> {
            String raw = stringRedisTemplate.opsForValue().getAndDelete(key);
            if (raw == null || raw.isBlank()) return null;
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return null;
            }
        }, null, "consumeRegistrationCount: key=" + key);
    }

    public void restoreRegistrationCount(String key, long delta) {
        if (key == null || key.isBlank() || delta <= 0) return;
        redisGuard.safeWrite(() -> {
            stringRedisTemplate.opsForValue().increment(key, delta);
            stringRedisTemplate.expire(key, addJitter(RedisKey.EVENT_REGISTRATION_COUNT.getTtl()));
        }, "restoreRegistrationCount: key=" + key + ", delta=" + delta);
    }

    public Map<Long, Long> getRegistrationCountsBatch(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();
        return redisGuard.safeRead(() -> {
            List<String> keys = eventIds.stream()
                    .map(RedisKey.EVENT_REGISTRATION_COUNT::build)
                    .collect(Collectors.toList());
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) return Collections.emptyMap();

            Map<Long, Long> counts = new HashMap<>(eventIds.size());
            for (int i = 0; i < eventIds.size() && i < values.size(); i++) {
                String raw = values.get(i);
                if (raw != null && !raw.isBlank()) {
                    try {
                        counts.put(eventIds.get(i), Long.parseLong(raw));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return counts;
        }, Collections.emptyMap(), "getRegistrationCountsBatch: size=" + eventIds.size());
    }
}
