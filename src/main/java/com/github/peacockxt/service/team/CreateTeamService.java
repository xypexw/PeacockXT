package com.github.peacockxt.service.team;

import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.TeamPayload;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.service.membership.CreateMembershipService;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import com.github.peacockxt.service.provider.UserProvider; // <-- Import Validator mới
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class CreateTeamService {
    
    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CreateMembershipService createMembershipService;
    private final UserProvider userProvider;
    private final LocalEventDispatcher localEventDispatcher;
    private final String DEFAULT_STATUS = "DEFAULT";
    private final String DEFAULT_ROLE = "OWNER";
    private final String event_name = "CREATE_TEAM";

    @Transactional
    public void createTeam(Long userId, String description, String name, String type) {
        
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User not exist");
        }
        
        Team team = teamBuilder(description, name, type, userId);
        teamRepository.save(team);
        createMembershipService.createMembership(userId, team.getTeamId(), DEFAULT_ROLE);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(team));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                teamChannelRedisDao.saveTeamInfo(team);
            }
        });
    }

    private Team teamBuilder(String description, String name, String type, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return Team.builder()
                .teamId(snowflakeIdGenerator.nextId())
                .name(name)
                .description(description)
                .type(type)
                .status(DEFAULT_STATUS)
                .createAt(now)
                .createBy(userId)
                .updateBy(userId)
                .updateAt(now)
                .build();
    }

    InternalEvent<TeamPayload> internalEventBuilder( Team team) {
        TeamPayload payload =  TeamPayload.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .type(team.getType())
                .status(team.getStatus())
                .status(team.getStatus())
                .build();
        return InternalEvent.<TeamPayload>builder()
                .payload(payload)
                .eventType(event_name)
                .destination(WebsocketDestination.TEAM.getDestination(team.getTeamId()))
                .build();
    }

}