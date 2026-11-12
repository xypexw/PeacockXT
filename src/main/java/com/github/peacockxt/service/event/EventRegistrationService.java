package com.github.peacockxt.service.event;


import com.github.peacockxt.dto.event.EventPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Event;
import com.github.peacockxt.repository.implementation.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EventRegistrationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final LocalEventDispatcher localEventDispatcher;
    private final EventRepository eventRepository;
    private final EventRedisDao eventRedisDao;

    private static final String EVENT_NAME = "REGISTRATION_EVENT";

    private static final String REGISTRATION_COUNT_KEY = "event_registration_count";

    public void registerForEvent( Long eventId , Long teamId ) {

        Event event = getEvent(eventId, teamId);
        if(event == null)
            throw new BusinessLogicException("Event not found");

        if(!teamId.equals(event.getTeam().getTeamId()) )
            throw new BusinessLogicException("Wrong team id");

        try{
            stringRedisTemplate.opsForValue().increment(REGISTRATION_COUNT_KEY + ":" + eventId, 1);
        }
        catch(Exception e){
            eventRepository.increasingAttended(eventId,1);
        }

        EventPayload payload = EventPayload.builder()
                .eventId(eventId).numberAttend(event.getNumberAttend() + 1 ).build();
        localEventDispatcher.dispatchToRabbitMQ(InternalEvent.
                <EventPayload>builder()
                        .payload(payload)
                        .eventType(EVENT_NAME)
                        .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build());

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



}