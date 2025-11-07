package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

public class UpdateTeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Team> teamRedisTemplate;
    private final HelperTeamService helperTeamService;

    public UpdateTeamService(TeamRepository teamRepository,
                             UserRepository userRepository,
                             RedisTemplate<String, Team> teamRedisTemplate,
                             HelperTeamService helperTeamService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamRedisTemplate = teamRedisTemplate;
        this.helperTeamService = helperTeamService;
    }

    @Transactional
    public void EditTeamService(String teamId , String name , String description , String type , String userId){
        LocalDateTime currentTime = LocalDateTime.now();
        Team team = teamRepository.findTeamByTeamId(teamId);
        team.setName(name);
        team.setDescription(description);
        team.setType(type);
        team.setUpdateAt(currentTime);
        team.setUpdateBy(userId);
        teamRepository.save(team);
        deleteFormCache(teamId);
    }

    private void deleteFormCache(String teamId) {
        String teamKey = helperTeamService.getTeamKey(teamId);
        teamRedisTemplate.delete(teamKey);
    }

}
