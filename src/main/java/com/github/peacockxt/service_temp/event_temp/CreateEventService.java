package com.github.peacockxt.service_temp.event_temp;

import com.github.peacockxt.dto_temp.event_temp.EventPayload;
import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.customexception_temp.BusinessLogicException;
import com.github.peacockxt.models_temp.Event;
import com.github.peacockxt.models_temp.Team;
import com.github.peacockxt.repository_temp.implementation.EventRepository;
import com.github.peacockxt.repository_temp.implementation.TeamRepository;
import com.github.peacockxt.infrastructure_temp.cache_temp.EventRedisDao;
import com.github.peacockxt.service_temp.RedisRetryManager;
import com.github.peacockxt.service_temp.SnowflakeIdGenerator;
import com.github.peacockxt.service_temp.provider_temp.TeamProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@Slf4j
@AllArgsConstructor
public class CreateEventService {

    private static final String DEFAULT_STATUS = "DEFAULT";

    private final LocalEventDispatcher localEventDispatcher;
    private final String event_name = "CREATE_EVENT";
    private final EventRepository eventRepository;
    private final EventRedisDao eventRedisDao;
    private final TeamRepository teamRepository;
    private final TeamProvider teamProvider;
    private final RedisRetryManager redisRetryManager;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Transactional
    public void createEvent(String title, String content, Long creatorId, LocalDateTime startTime,
                            LocalDateTime endTime, Long teamId) {

        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        Event event = eventBuilder(title, content, creatorId, startTime, endTime, teamId);
        eventRepository.save(event);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(event, teamId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try{
                    eventRedisDao.saveEvent(teamId, event);
                }
                catch (Exception e){
                    redisRetryManager.execute( () -> eventRedisDao.saveEvent(teamId, event) );
                }
            }
        });
    }

    private Event eventBuilder(String title, String content, Long creatorId, LocalDateTime startTime,
                               LocalDateTime endTime, Long teamId) {
        Team team = teamRepository.getReferenceById(teamId);
        return Event.builder()
                .eventId(snowflakeIdGenerator.nextId())
                .title(title)
                .content(content)
                .createAt(LocalDateTime.now())
                .numberAttend(0)
                .startTime(startTime)
                .endTime(endTime)
                .status(DEFAULT_STATUS)
                .createBy(creatorId)
                .team(team)
                .build();
    }

    private InternalEvent<EventPayload> internalEventBuilder(Event event , Long teamId ) {
        EventPayload payload = EventPayload.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .content(event.getContent())
                .createAt(event.getCreateAt())
                .numberAttend(0)
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(DEFAULT_STATUS)
                .build();
        return InternalEvent.<EventPayload>builder()
                .eventType(event_name)
                .payload(payload)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }

}