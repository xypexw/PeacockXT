package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.Event;
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
public class EventRedisDao {

    // Limit to the latest 500 events per Team

    private static final int MAX_INDEX_SIZE = 500;
    private final RedisTemplate<String, Event> eventRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    // --- HELPER: Add Jitter to TTL (Random from 0 to 30 minutes) ---
    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(1800);
        return baseTtl.plusSeconds(jitterSeconds);
    }

    // ==========================================
    // 1. ADD / UPDATE EVENT (WRITE -> safeWrite)
    // ==========================================
    public void saveEvent(Long teamId, Event event) {
        redisGuard.safeWrite(() -> {
            Long eventId = event.getEventId();

            // 1. Persist Event Data
            String dataKey = RedisKey.EVENT_DATA.build(eventId);
            Duration ttl = addJitter(RedisKey.EVENT_DATA.getTtl());
            eventRedisTemplate.opsForValue().set(dataKey, event, ttl);

            // 2. Add Event Index to Team's ZSet
            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);
            longRedisTemplate.opsForZSet().add(indexKey, eventId, eventId.doubleValue());

            // 3. Trim index to retain only the latest 500 events
            longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
        }, "saveEvent: teamId=" + teamId + ", eventId=" + event.getEventId());
    }

    public void saveEventBatch(Long teamId, List<Event> events) {
        if (events == null || events.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            Map<String, Event> eventMap = new HashMap<>();
            for (Event event : events) {
                eventMap.put(RedisKey.EVENT_DATA.build(event.getEventId()), event);
            }
            redisBatch.multiSetWithTtl(eventRedisTemplate, eventMap, addJitter(RedisKey.EVENT_DATA.getTtl()));
        }, "saveEventBatch: teamId=" + teamId + ", size=" + events.size());
    }

    public void saveEventIdsBatch(Long teamId, List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return;
        redisGuard.safeWrite(() -> {
            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);
            Set<ZSetOperations.TypedTuple<Long>> tuples = eventIds.stream()
                    .map(eventId -> new DefaultTypedTuple<>(eventId, eventId.doubleValue()))
                    .collect(Collectors.toSet());
            longRedisTemplate.opsForZSet().add(indexKey, tuples);
            longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
        }, "saveEventIdsBatch: teamId=" + teamId + ", size=" + eventIds.size());
    }

    // ==========================================
    // 2. RETRIEVE EVENT - SINGLE & BATCH (READ -> safeRead)
    // ==========================================
    public Event getEventData(Long eventId) {
        return redisGuard.safeRead(() -> {
            String dataKey = RedisKey.EVENT_DATA.build(eventId);
            return eventRedisTemplate.opsForValue().get(dataKey);
        }, null, "getEventData: eventId=" + eventId);
    }

    public List<Event> getEventsBatch(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyList();
        return redisGuard.safeRead(() -> {
            List<String> keys = eventIds.stream()
                    .map(RedisKey.EVENT_DATA::build)
                    .collect(Collectors.toList());

            List<Event> events = eventRedisTemplate.opsForValue().multiGet(keys);
            if( events == null || events.isEmpty() ) return Collections.emptyList();
            else return  events;
        }, Collections.emptyList(), "getEventsBatch: size=" + eventIds.size());
    }

    // ==========================================
    // 3. CURSOR - EVENT PAGINATION (READ -> safeRead)
    // ==========================================
    public Set<Long> getEventIdsBeforeCursor(Long teamId, Long cursorEventId, int limit) {
        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.TEAM_EVENT_INDEX.build(teamId);

            if (cursorEventId == null) {
                // Initial load: fetch the most recent events
                return longRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
            } else {
                // Subsequent load: fetch older events before cursor
                return longRedisTemplate.opsForZSet().reverseRangeByScore(
                        indexKey, 0, cursorEventId - 1, 0, limit
                );
            }
        }, Collections.emptySet(), "getEventIdsBeforeCursor: teamId=" + teamId + ", cursor=" + cursorEventId);
    }

    // ==========================================
    // 4. DELETE EVENT (WRITE/DELETE -> safeWrite)
    // ==========================================
    public void deleteEvent(Long teamId, Long eventId) {
        redisGuard.safeWrite(() -> {
            eventRedisTemplate.delete(RedisKey.EVENT_DATA.build(eventId));
            longRedisTemplate.opsForZSet().remove(RedisKey.TEAM_EVENT_INDEX.build(teamId), eventId);
        }, "deleteEvent: teamId=" + teamId + ", eventId=" + eventId);
    }

}