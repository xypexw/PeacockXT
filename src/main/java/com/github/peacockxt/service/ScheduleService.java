package com.github.peacockxt.service;

import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.repository.implementation.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private static final String REG_COUNT_PREFIX = "peacockxt:event:";
    private static final String REG_COUNT_SUFFIX = ":reg_count";

    private final EventRedisDao eventRedisDao;
    private final EventRepository eventRepository;

    @Scheduled(fixedDelay = 60_000)
    public void flushEventRegistrationCounts() {
        Set<String> keys = eventRedisDao.scanRegistrationCountKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            Long eventId = extractEventId(key);
            if (eventId == null) {
                continue;
            }

            Long delta = eventRedisDao.consumeRegistrationCount(key);
            if (delta == null || delta <= 0) {
                continue;
            }

            try {
                long remaining = delta;
                while (remaining > 0) {
                    int batch = (int) Math.min(remaining, Integer.MAX_VALUE);
                    eventRepository.increasingAttended(eventId, batch);
                    remaining -= batch;
                }
                eventRedisDao.deleteEventData(eventId);
                log.debug("Flushed {} registration(s) to DB for eventId={}", delta, eventId);
            } catch (Exception e) {
                log.error("Failed to flush {} registration(s) to DB for eventId={}. Restoring counter in Redis.",
                        delta, eventId, e);
                eventRedisDao.restoreRegistrationCount(key, delta);
            }
        }
    }

    private Long extractEventId(String key) {
        if (key == null || !key.startsWith(REG_COUNT_PREFIX) || !key.endsWith(REG_COUNT_SUFFIX)) {
            return null;
        }
        String idPart = key.substring(REG_COUNT_PREFIX.length(), key.length() - REG_COUNT_SUFFIX.length());
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            log.warn("Ignoring malformed registration count key: {}", key);
            return null;
        }
    }
}
