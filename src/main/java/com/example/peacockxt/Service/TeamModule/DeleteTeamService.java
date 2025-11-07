package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteTeamService {

    private final TeamRepository teamRepository;
    private final RedisTemplate<String, Team> teamRedisTemplate;
    private final HelperTeamService helperTeamService;

    public DeleteTeamService(TeamRepository teamRepository,
                             RedisTemplate<String, Team> teamRedisTemplate,
                             HelperTeamService helperTeamService) {
        this.teamRepository = teamRepository;
        this.teamRedisTemplate = teamRedisTemplate;
        this.helperTeamService = helperTeamService;
    }

    private void deleteTeamFromCache(String teamId) {
        String teamKey = helperTeamService.getTeamKey(teamId);
        teamRedisTemplate.delete(teamKey);
    }

    private void deleteTeamFromDatabase(String teamId) {
        Optional<Team> team = teamRepository.findById(teamId);
        team.ifPresent(teamRepository::delete);
    }

    @Transactional
    public void deleteTeam(String teamId) {
        deleteTeamFromCache(teamId);
        deleteTeamFromDatabase(teamId);
    }
}
