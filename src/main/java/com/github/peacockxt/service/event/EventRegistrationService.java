package com.github.peacockxt.service.event;

import com.github.peacockxt.dto.event.EventPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheEvent;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Event;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EventRegistrationService {

    private final LocalEventDispatcher localEventDispatcher;
    private final EventRepository eventRepository;
    private final EventRedisDao eventRedisDao;

    private static final String EVENT_NAME = "REGISTRATION_EVENT";

    public void registerForEvent(Long eventId, Long teamId) {

        CacheEvent event = getEvent(eventId, teamId);
        if (event == null)
            throw new BusinessLogicException("Event not found");

        if (!teamId.equals(event.getTeamId()))
            throw new BusinessLogicException("Wrong team id");

        try {
            eventRedisDao.incrementRegistrationCount(eventId);
        } catch (Exception e) {
            eventRepository.increasingAttended(eventId, 1);
        }

        int currentAttend = event.getNumberAttend() != null ? event.getNumberAttend() : 0;
        EventPayload payload = EventPayload.builder()
                .eventId(eventId).numberAttend(currentAttend + 1).build();
        localEventDispatcher.dispatchToRabbitMQ(InternalEvent.<EventPayload>builder()
                .payload(payload)
                .eventType(EVENT_NAME)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build());
    }

    private CacheEvent getEvent(Long eventId, Long teamId) {
        CacheEvent cached = eventRedisDao.getEventData(eventId);
        if (cached != null) {
            return cached;
        }
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            CacheEvent cacheEvent = CacheEvent.builder()
                    .eventId(event.getEventId())
                    .title(event.getTitle())
                    .content(event.getContent())
                    .status(event.getStatus())
                    .createBy(event.getCreateBy())
                    .createAt(event.getCreateAt())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .numberAttend(event.getNumberAttend())
                    .teamId(event.getTeamId())
                    .build();
            eventRedisDao.saveEventBatch(teamId, List.of(cacheEvent));
            return cacheEvent;
        }
        return null;
    }
}
