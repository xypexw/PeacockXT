package com.example.peacockxt.Service.Event;

import com.example.peacockxt.DTO.Respone.EventResponse;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Event;
import com.example.peacockxt.Repository.Imlpementation.EventRepository;
import com.example.peacockxt.Infrastructure.Cache.EventRedisDao;
import com.example.peacockxt.Service.Provider.TeamProvider;
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
    private static final String DEFAULT_STATUS = "DEFAULT";
    private static final String DELETED_STATUS = "DELETED";
    public List<EventResponse> readEvent(Long teamId, Long cursorId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        List<Long> indexes = getEventIds(teamId, cursorId);
        if (indexes.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventResponse> responses = new ArrayList<>();

        List<Event> result = getEvents(teamId, indexes);

        for (Event event : result) {
            if (event == null || DELETED_STATUS.equals(event.getStatus())) {
                continue;
            }
            responses.add(mapToResponse(event));
        }

        return responses;
    }

    private List<Event> getEvents(Long teamId, List<Long> messageIds) {
        List<Event> eventsList = getEventsInCache(messageIds);
        List<Long> eventIdsNeedToFill = new ArrayList<>();

        if (eventsList == null || eventsList.size() != messageIds.size()) {
            eventsList = new ArrayList<>(Collections.nCopies(messageIds.size(), null));
        }

        for (int i = 0; i < messageIds.size(); i++) {
            if (eventsList.get(i) == null) {
                eventIdsNeedToFill.add(messageIds.get(i));
            }
        }

        // If 100% cache hit, return immediately without querying the database
        if (eventIdsNeedToFill.isEmpty()) {
            return eventsList;
        }

        Map<Long, Event> eventPool = new HashMap<>();
        List<Event> missingEvents = fetchEventInDatabase(teamId, eventIdsNeedToFill);

        for (Event event : missingEvents) {
            eventPool.put(event.getEventId(), event);
        }

        eventRedisDao.saveEventBatch(teamId,missingEvents);

        List<Event> result = new ArrayList<>(messageIds.size());
        for (int i = 0; i < messageIds.size(); i++) {
            Event event = eventsList.get(i);
            if (event == null) {
                Long index = messageIds.get(i);
                event = eventPool.get(index);
            }

            if (event != null && !DELETED_STATUS.equals(event.getStatus())) {
                result.add(event);
            }
        }
        return result;
    }

    private List<Event> getEventsInCache(List<Long> eventIds) {
        return eventRedisDao.getEventsBatch(eventIds);
    }

    private List<Event> fetchEventInDatabase(Long teamId, List<Long> eventIds) {
        return eventRepository.fetchEventBatchByEventId(teamId, eventIds);
    }

    private List<Long> getEventIds(Long teamId, Long cursorId) {
        List<Long> eventIdsInCache = convertSetToList(fetchEventIdsInCache(teamId, cursorId));

        if (eventIdsInCache.isEmpty()) {
            if (cursorId == null) {
                Long effectiveCursor = Long.MAX_VALUE;
                Pageable pageable = PageRequest.of(0, DEFAULT_LIMIT);
                List<Long> indexes = eventRepository.getIndexByTeamIdAndCursorId(teamId,effectiveCursor,pageable);
                eventRedisDao.saveEventIdsBatch(teamId, indexes);;
                int toIndex = Math.min(indexes.size(), PAGE_SIZE);
                return indexes.subList(0, toIndex);
            } else {
                return eventRepository.getIndexByTeamIdAndCursorId(teamId, cursorId,PAGE_REQUEST);
            }
        }
        return eventIdsInCache;
    }

    private List<Long> convertSetToList(Set<Long> set) {
        return new ArrayList<>(set);
    }

    private Set<Long> fetchEventIdsInCache(Long teamId, Long cursorId) {
        // Retrieve only PAGE_SIZE (20) items using cursor from Redis instead of fetching 500 IDs
        return eventRedisDao.getEventIdsBeforeCursor(teamId, cursorId, PAGE_SIZE);
    }

    private EventResponse mapToResponse(Event event) {
        if (event == null) {
            return null;
        }
        return EventResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .content(event.getContent())
                .status(event.getStatus())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .createAt(event.getCreateAt())
                .createBy(event.getCreateBy())
                .numberAttend(event.getNumberAttend())
                .build();
    }


}