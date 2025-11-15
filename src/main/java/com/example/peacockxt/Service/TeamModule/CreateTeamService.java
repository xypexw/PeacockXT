package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
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
    private static final int TTL_DAYS = 14;

    public CreateTeamService(TeamRepository teamRepository,
                             RedisTemplate<String, Team> teamRedisTemplate,
                             HelperTeamService helperTeamService) {
        this.teamRepository = teamRepository;
        this.teamRedisTemplate = teamRedisTemplate;
        this.helperTeamService = helperTeamService;
    }

    private void saveTeamInCache(Team team) {
        try {
            String teamKey = helperTeamService.getTeamKey(team.getTeamId());
            teamRedisTemplate
                    .opsForValue()
                    .setIfAbsent(teamKey, team, TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new CacheAccessException("Cache access error", e);
        }
    }

    private void saveTeamInDatabase(Team team) {
        try {
            teamRepository.save(team);
        } catch (Exception e) {
            throw new DatabaseException("Database error", e);
        }
    }

    @Transactional
    public String createTeam(String teamName,
                             String description,
                             String type,
                             String config,
                             String userId) {

        try {
            String teamId = UuidCreator.getTimeBased().toString();
            Team team = teamBuilder(teamId, teamName, description, type, config, userId);
            try {
                saveTeamInCache(team);
            } catch (CacheAccessException e) {
                System.err.println("Cache error for teamId=" + teamId + ": " + e.getMessage());
            }
            saveTeamInDatabase(team);
            return teamId;
        } catch (DatabaseException e) {
            throw new BusinessLogicException("Cannot create team due to database error", e);

        } catch (Exception e) {
            throw new BusinessLogicException("Logic failed", e);
        }
    }

    private Team teamBuilder(String teamId,
                             String teamName,
                             String teamDescription,
                             String type,
                             String config,
                             String userId) {

        final String defaultStatus = "DEFAULT";
        final LocalDateTime now = LocalDateTime.now();

        return Team.builder()
                .teamId(teamId)
                .name(teamName)
                .description(teamDescription)
                .status(defaultStatus)
                .type(type)
                .createAt(now)
                .updateAt(now)
                .createBy(userId)
                .updateBy(userId)
                .build();
    }
}
