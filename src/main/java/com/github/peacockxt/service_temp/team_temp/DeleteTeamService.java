package com.github.peacockxt.service_temp.team_temp;

import java.time.LocalDateTime;

import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.dto_temp.event_temp.TeamPayload;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.service_temp.provider_temp.MembershipProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import com.github.peacockxt.models_temp.Team;
import com.github.peacockxt.models_temp.customexception_temp.BusinessLogicException;
import java.util.List;
import com.github.peacockxt.repository_temp.implementation.TeamRepository;
import com.github.peacockxt.infrastructure_temp.cache_temp.TeamChannelRedisDao;
import com.github.peacockxt.service_temp.provider_temp.TeamProvider;
import com.github.peacockxt.service_temp.provider_temp.UserProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class DeleteTeamService {
    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final MembershipProvider membershipProvider;
    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final String DELETED_TEAM = "DELETED";
    private final String event_name = "DELETE_TEAM";

    @Transactional
    public void deleteTeam(Long userId , Long teamId , String role ){
        if(!userProvider.hasUser(userId)){
            throw new BusinessLogicException("User is not exist");
        }

        if( !role.equals("OWNER") && !role.equals("ADMIN")  ){
            throw new BusinessLogicException("Invalid role");
        }

        Team team = teamProvider.getTeam(teamId);
        if(team==null) throw new BusinessLogicException("Team is not exist");

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
                }
            });
        } else {
            teamChannelRedisDao.deleteTeamInfo(team.getTeamId());
            teamChannelRedisDao.removeAllChannelIndexesInTeam(teamId);
        }
    }

    private void removeAllUserInTeam(Long teamId){
        List<Long>  membershipIds = membershipProvider.getMembershipIdsFromTeam(teamId);
        membershipProvider.deleteBulkMembership(membershipIds,teamId);
    }

    InternalEvent<TeamPayload> internalEventBuilder(Team team){
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
