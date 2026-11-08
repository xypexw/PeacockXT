package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Repository.Imlpementation.TeamRepository;
import com.example.peacockxt.Infrastructure.Cache.TeamChannelRedisDao;
import com.example.peacockxt.Service.RedisRetryManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TeamProvider {

    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final RedisRetryManager redisRetryManager;

    public boolean hasTeam(Long teamId) {
        Team team = hasTeamInCache(teamId);
        if (team != null) return true;

        try {
            team = teamRepository.getTeamByTeamId(teamId);
            if (team == null) return false;

            saveTeamInfoToCache(team);
            return true;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Team from database", e);
        }
    }

    public Team getTeam(Long teamId) {
        Team team = hasTeamInCache(teamId);
        if (team != null) return team;

        try {
            team = teamRepository.getTeamByTeamId(teamId);
            if (team != null) saveTeamInfoToCache(team);
            return team;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Team from database", e);
        }
    }

    private Team hasTeamInCache(Long teamId) {
        try {
            return teamChannelRedisDao.getTeamInfo(teamId);
        } catch (Exception e) {
            log.error("Failed to read Team from Redis: {}", e.getMessage(), e);
            return null;
        }
    }

    private void saveTeamInfoToCache(Team team) {
        try {
            teamChannelRedisDao.saveTeamInfo(team);
        } catch (Exception e) {
            log.warn("Failed to cache team info, adding task to retry manager: {}", e.getMessage());
            redisRetryManager.execute(() -> teamChannelRedisDao.saveTeamInfo(team));
        }
    }

}