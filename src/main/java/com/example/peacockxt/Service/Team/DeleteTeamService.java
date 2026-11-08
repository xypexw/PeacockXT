package com.example.peacockxt.Service.Team;

import java.time.LocalDateTime;

import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.TeamPayload;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Service.Provider.MembershipProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import java.util.List;
import com.example.peacockxt.Repository.Imlpementation.TeamRepository;
import com.example.peacockxt.Infrastructure.Cache.TeamChannelRedisDao;
import com.example.peacockxt.Service.Provider.TeamProvider;
import com.example.peacockxt.Service.Provider.UserProvider;
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
