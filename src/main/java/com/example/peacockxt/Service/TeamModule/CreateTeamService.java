package com.example.peacockxt.Service.TeamModule;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class CreateTeamService {
    private final TeamRepository teamRepository;
    private final RedisTemplate<String, Team> teamRedisTemplate;
    private final HelperTeamService helperTeamService;
    private final int timeToLive = 14;

    public CreateTeamService(TeamRepository teamRepository,
                             RedisTemplate<String,Team> teamRedisTemplate,
                             HelperTeamService helperTeamService) {
        this.teamRepository = teamRepository;
        this.teamRedisTemplate = teamRedisTemplate;
        this.helperTeamService = helperTeamService;
    }

    private void saveTeamInCache(Team team) {
        String teamKey = helperTeamService.getTeamKey(team.getTeamId());
        teamRedisTemplate.opsForValue().setIfAbsent(teamKey, team ,timeToLive, TimeUnit.DAYS);
    }

    private void saveTeamInDatabase(Team team) {
        teamRepository.save(team);
    }

    @Transactional
    public void CreateTeam(String teamName , String description , String type , String config , String userId){
        String teamId = UuidCreator.getTimeBased().toString();
        Team team = teamBuilder(teamId,teamName,description,type,config,userId);
        saveTeamInCache(team);
        saveTeamInDatabase(team);
    }

    private Team teamBuilder(String teamId, String teamName , String teamDescription
            , String type , String config , String userId){
        final String defaultStatus = "DEFAULT";
        final LocalDateTime currentTime = LocalDateTime.now();
        return Team.builder()
                .teamId(teamId)
                .name(teamName)
                .description(teamDescription)
                .status(defaultStatus)
                .type(type)
                .updateAt(currentTime)
                .createAt(currentTime)
                .createBy(userId)
                .updateBy(userId)
                .status(defaultStatus).build();
    }

}
