package com.example.peacockxt.Service.Event;

import com.example.peacockxt.DTO.Event.EventPayload;
import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.MessagePayload;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Event;
import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Repository.Imlpementation.EventRepository;
import com.example.peacockxt.Repository.Imlpementation.TeamRepository;
import com.example.peacockxt.Infrastructure.Cache.EventRedisDao;
import com.example.peacockxt.Service.RedisRetryManager;
import com.example.peacockxt.Service.SnowflakeIdGenerator;
import com.example.peacockxt.Service.Provider.TeamProvider;
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