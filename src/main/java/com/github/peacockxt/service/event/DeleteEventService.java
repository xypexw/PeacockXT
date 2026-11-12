package com.github.peacockxt.service.event;

import com.github.peacockxt.dto.event.EventPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Event;
import com.github.peacockxt.repository.implementation.EventRepository;
import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.RedisRetryManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class DeleteEventService {

    private static final String STATUS_DELETED = "DELETED";
    private final String event_name = "DELETE_EVENT";
    private final LocalEventDispatcher localEventDispatcher;
    private final EventRepository eventRepository;
    private final EventRedisDao eventRedisDao;
    private final TeamProvider teamProvider;
    private final RedisRetryManager redisRetryManager;

    @Transactional
    public void deleteEvent(Long teamId, Long eventId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        Event event = getEvent(eventId, teamId);
        if (event == null) {
            throw new BusinessLogicException("Event does not exist");
        }

        event.setStatus(STATUS_DELETED);
        eventRepository.save(event);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(event, teamId));


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFromCache(teamId, eventId);
            }
        });
    }

    private Event getEvent(Long eventId , Long teamId) {
        Event event = eventRedisDao.getEventData(eventId);
        if (event != null) {
            return event;
        }
        event = eventRepository.findById(eventId).orElse(null);
        if( event != null ) {
            eventRedisDao.saveEventBatch(teamId, List.of(event));
        }
        return event;
    }

    private InternalEvent<EventPayload> internalEventBuilder(Event event , Long teamId ) {
        EventPayload payload = EventPayload.builder()
                .eventId(event.getEventId())
                .status(STATUS_DELETED)
                .build();
        return InternalEvent.<EventPayload>builder()
                .eventType(event_name)
                .payload(payload)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }

    private void deleteFromCache(Long teamId, Long eventId) {
        eventRedisDao.deleteEvent(teamId, eventId);
    }
}