package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.GroupModule.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TeamCacheDeleterService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final HelperTeamService helperTeamService;
    private final RedisTemplate<String, Team> teamRedisTemplate;

    public TeamCacheDeleterService(HelperTeamService helperTeamService, RedisTemplate<String, Team> teamRedisTemplate) {
        this.helperTeamService = helperTeamService;
        this.teamRedisTemplate = teamRedisTemplate;
    }

    void deleteTeamFromCache(String teamId) {
        try {
            String teamKey = helperTeamService.getTeamKey(teamId);
            Boolean result = teamRedisTemplate.delete(teamKey);
            if (result) {
                logger.info("Cache Delete: team {} removed from cache successfully", teamId);
            } else {
                logger.warn("Cache Delete: team {} not found in cache", teamId);
            }
        } catch (Exception e) {
            logger.error("Cache Delete Failed: error removing team {} from cache", teamId, e);
            throw new CacheAccessException("Failed to delete team from cache", e);
        }
    }
}
