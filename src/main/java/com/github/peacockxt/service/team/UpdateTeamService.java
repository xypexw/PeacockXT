package com.github.peacockxt.service.team;

import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.TeamPayload;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
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
public class UpdateTeamService {
    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final UserProvider userProvider;
    private final TeamProvider teamProvider;

    private final String event_name = "UPDATE_TEAM";
    private final String UPDATED_STATUS = "UPDATED";

    @Transactional
    public void updateTeamService(Long teamId, Long userId, String description, String type) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist or is invalid");
        }

        Team team = teamProvider.getTeam(teamId);
        if (team == null) {
            throw new BusinessLogicException("Team does not exist");
        }

        team.setDescription(description);
        team.setType(type);
        team.setStatus(UPDATED_STATUS);
        team.setUpdateBy(userId);
        team.setUpdateAt(LocalDateTime.now());

        teamRepository.save(team);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    teamChannelRedisDao.saveTeamInfo(team);
                }
            });
        } else {
            teamChannelRedisDao.saveTeamInfo(team);
        }
    }

    InternalEvent<TeamPayload> internalEventBuilder(Team team) {
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