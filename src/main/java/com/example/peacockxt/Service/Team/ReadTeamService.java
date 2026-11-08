package com.example.peacockxt.Service.Team;
import com.example.peacockxt.DTO.Respone.TeamResponse;
import com.example.peacockxt.Infrastructure.Cache.TeamChannelRedisDao;
import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Repository.Imlpementation.TeamRepository;
import com.example.peacockxt.Service.Provider.UserProvider;
import com.example.peacockxt.Service.RedisRetryManager;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
@AllArgsConstructor
public class ReadTeamService{
    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao  teamChannelRedisDao;
    private final RedisRetryManager redisRetryManager;
    private final UserProvider  userProvider;

    public List<TeamResponse> getTeamForBulkAPI(List<Long> teamIds) {
        List<Team> teamsInCache = teamChannelRedisDao.getAllTeamInfo(teamIds);
        List<Long> missingIds = new ArrayList<>();
        for (int i = 0; i < teamIds.size(); i++) {
            if(teamsInCache.get(i)==null){
                missingIds.add(teamIds.get(i));
            }
        }

        if(missingIds.isEmpty()) return responses(teamsInCache);

        List<Team> membershipsFromDatabase = fetchAndCacheMissingTeams(missingIds);
        Map<Long, Team> teamMap = new HashMap<>();
        for (Team team : membershipsFromDatabase) {
            teamMap.put(team.getTeamId(), team);
        }

        List<Team> result = new ArrayList<>(teamIds.size());

        for(int i = 0; i < teamIds.size(); i++){
            Team team = teamsInCache.get(i);
            if(team!=null){
                result.add(team);
            }
            else {
                Team teamFromDb = teamMap.get(teamIds.get(i));
                if(teamFromDb!=null){
                    result.add(teamFromDb);
                }
            }
        }

        return responses(result);
    }

    private List<Team> fetchAndCacheMissingTeams(List<Long> teamIds) {
        List<Team> teams =  teamRepository.getTeamsByTeamIds(teamIds);
        if(!teams.isEmpty()){
            redisRetryManager.execute(() -> teamChannelRedisDao.saveTeamInfoBatch(teams));
        }
        return teams;
    }

    List<TeamResponse> responses(List<Team> teams) {
        List<TeamResponse> responses = new ArrayList<>();
        for (Team team : teams) {
            if(!team.getStatus().equals("DELETED")){
                responses.add(toResponse(team));
            }
        }
        return responses;
    }


    private TeamResponse toResponse(Team team) {
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .status(team.getStatus())
                .type(team.getType())
                .build();
    }

}

