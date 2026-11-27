package com.github.peacockxt.service.event;

import com.github.peacockxt.dto.response.EventResponse;
import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheEvent;
import com.github.peacockxt.models.Event;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.EventRepository;
import com.github.peacockxt.service.provider.TeamProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReadEventService {

    private final EventRepository eventRepository;
    private final EventRedisDao eventRedisDao;
    private final TeamProvider teamProvider;
    private static final int PAGE_SIZE = 20;
    private static final PageRequest PAGE_REQUEST = PageRequest.of(0, PAGE_SIZE);
    private static final int DEFAULT_LIMIT = 500;
    private static final String DELETED_STATUS = "DELETED";

    public List<EventResponse> readEvent(Long teamId, Long cursorId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        List<Long> indexes = getEventIds(teamId, cursorId);
        if (indexes.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheEvent> result = getEvents(teamId, indexes);
        Map<Long, Long> regCounts = eventRedisDao.getRegistrationCountsBatch(indexes);

        List<EventResponse> responses = new ArrayList<>(result.size());
        for (CacheEvent event : result) {
            if (event == null || DELETED_STATUS.equals(event.getStatus())) {
                continue;
            }
            long delta = regCounts.getOrDefault(event.getEventId(), 0L);
            responses.add(mapToResponse(event, delta));
        }

        return responses;
    }

    private List<CacheEvent> getEvents(Long teamId, List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheEvent> cached = eventRedisDao.getEventsBatch(eventIds);
        Map<Long, CacheEvent> eventPool = new HashMap<>(eventIds.size());
        if (cached != null) {
            for (CacheEvent e : cached) {
                if (e != null) {
                    eventPool.put(e.getEventId(), e);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : eventIds) {
            if (!eventPool.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Event> missingEvents = eventRepository.fetchEventBatchByEventId(teamId, missingIds);
            List<CacheEvent> toCache = new ArrayList<>(missingEvents.size());
            for (Event entity : missingEvents) {
                CacheEvent cacheEvent = toCacheEvent(entity);
                eventPool.put(cacheEvent.getEventId(), cacheEvent);
                toCache.add(cacheEvent);
            }
            if (!toCache.isEmpty()) {
                eventRedisDao.saveEventBatch(teamId, toCache);
            }
        }

        List<CacheEvent> result = new ArrayList<>(eventIds.size());
        for (Long id : eventIds) {
            CacheEvent event = eventPool.get(id);
            if (event != null && !DELETED_STATUS.equals(event.getStatus())) {
                result.add(event);
            }
        }
        return result;
    }

    private List<Long> getEventIds(Long teamId, Long cursorId) {
        Set<Long> cachedSet = eventRedisDao.getEventIdsBeforeCursor(teamId, cursorId, PAGE_SIZE);
        if (cachedSet != null && !cachedSet.isEmpty()) {
            return new ArrayList<>(cachedSet);
        }

        if (cursorId == null) {
            Pageable pageable = PageRequest.of(0, DEFAULT_LIMIT);
            List<Long> indexes = eventRepository.getIndexByTeamIdAndCursorId(teamId, Long.MAX_VALUE, pageable);
            if (indexes == null || indexes.isEmpty()) {
                return Collections.emptyList();
            }
            eventRedisDao.saveEventIdsBatch(teamId, indexes);
            return indexes.subList(0, Math.min(indexes.size(), PAGE_SIZE));
        } else {
            List<Long> indexes = eventRepository.getIndexByTeamIdAndCursorId(teamId, cursorId, PAGE_REQUEST);
            return (indexes == null) ? Collections.emptyList() : indexes;
        }
    }

    private EventResponse mapToResponse(CacheEvent event, long registrationDelta) {
        int baseAttend = event.getNumberAttend() != null ? event.getNumberAttend() : 0;
        return EventResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .content(event.getContent())
                .status(event.getStatus())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .createAt(event.getCreateAt())
                .createBy(event.getCreateBy())
                .numberAttend(baseAttend + (int) registrationDelta)
                .build();
    }

    private CacheEvent toCacheEvent(Event entity) {
        return CacheEvent.builder()
                .eventId(entity.getEventId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createAt(entity.getCreateAt())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .numberAttend(entity.getNumberAttend())
                .teamId(entity.getTeamId())
                .build();
    }
}
