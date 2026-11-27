package com.github.peacockxt.service.team;

import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.TeamPayload;
import com.github.peacockxt.infrastructure.cache.EmojiRedisDao;
import com.github.peacockxt.infrastructure.cache.EventRedisDao;
import com.github.peacockxt.infrastructure.cache.PostRedisDao;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.UserProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DeleteTeamService {
    private final UserProvider userProvider;
    private final MembershipProvider membershipProvider;
    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final PostRedisDao postRedisDao;
    private final EventRedisDao eventRedisDao;
    private final EmojiRedisDao emojiRedisDao;
    private final String DELETED_TEAM = "DELETED";
    private final String event_name = "DELETE_TEAM";

    @Transactional
    public void deleteTeam(Long userId, Long teamId, String role) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User is not exist");
        }

        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessLogicException("Invalid role");
        }

        Team team = teamRepository.getTeamByTeamId(teamId);
        if (team == null) throw new BusinessLogicException("Team is not exist");

        team.setStatus(DELETED_TEAM);
        team.setUpdateBy(userId);
        team.setUpdateAt(LocalDateTime.now());

        teamRepository.save(team);
        removeAllUserInTeam(teamId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    teamChannelRedisDao.deleteTeamInfo(team.getTeamId());
                    teamChannelRedisDao.removeAllChannelIndexesInTeam(teamId);
                    postRedisDao.deleteTeamPostIndex(teamId);
                    eventRedisDao.deleteTeamEventIndex(teamId);
                    emojiRedisDao.deleteAllTeamEmojis(teamId);
                }
            });
        } else {
            teamChannelRedisDao.deleteTeamInfo(team.getTeamId());
            teamChannelRedisDao.removeAllChannelIndexesInTeam(teamId);
            postRedisDao.deleteTeamPostIndex(teamId);
            eventRedisDao.deleteTeamEventIndex(teamId);
            emojiRedisDao.deleteAllTeamEmojis(teamId);
        }
    }

    private void removeAllUserInTeam(Long teamId) {
        List<Long> membershipIds = membershipProvider.getMembershipIdsFromTeam(teamId);
        membershipProvider.deleteBulkMembership(membershipIds, teamId);
    }

    InternalEvent<TeamPayload> internalEventBuilder(Team team) {
        TeamPayload payload = TeamPayload.builder()
                .teamId(team.getTeamId())
                .status(team.getStatus())
                .build();
        return InternalEvent.<TeamPayload>builder()
                .payload(payload)
                .eventType(event_name)
                .destination(WebsocketDestination.TEAM.getDestination(team.getTeamId()))
                .build();
    }
}
