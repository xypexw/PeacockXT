package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UpdateTeamService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateTeamService.class);

    private final TeamRepository teamRepository;
    private final TeamCacheDeleterService teamCacheDeleterService;

    public UpdateTeamService(TeamRepository teamRepository, TeamCacheDeleterService teamCacheDeleterService) {
        this.teamRepository = teamRepository;
        this.teamCacheDeleterService = teamCacheDeleterService;
    }

    @Transactional
    public void editTeam(String teamId, String name, String description, String type, String userId) {
        try {
            // Update team in database
            updateTeamInDatabase(teamId, name, description, type, userId);

            // Attempt to clear cache (non-critical operation)
            invalidateTeamCache(teamId);

        } catch (DatabaseException e) {
            logger.error("Failed to update team {} in database", teamId, e);
            throw new BusinessLogicException("Failed to update team: " + teamId, e);
        } catch (Exception e) {
            logger.error("Unexpected error while updating team {}", teamId, e);
            throw new BusinessLogicException("Unexpected error occurred while updating team: " + teamId, e);
        }
    }

    private void updateTeamInDatabase(String teamId, String name, String description, String type, String userId) {
        try {
            Team team = teamRepository.findTeamByTeamId(teamId);

            if (team == null) {
                throw new DatabaseException("Team not found with id: " + teamId);
            }

            LocalDateTime currentTime = LocalDateTime.now();
            team.setName(name);
            team.setDescription(description);
            team.setType(type);
            team.setUpdateAt(currentTime);
            team.setUpdateBy(userId);

            teamRepository.save(team);
            logger.info("Team {} updated successfully in database", teamId);

        } catch (DatabaseException e) {
            throw e; // Re-throw custom exceptions
        } catch (Exception e) {
            logger.error("Database error while updating team {}", teamId, e);
            throw new DatabaseException("Database error while updating team: " + teamId, e);
        }
    }

    private void invalidateTeamCache(String teamId) {
        try {
            teamCacheDeleterService.deleteTeamFromCache(teamId);
            logger.debug("Cache invalidated for team {}", teamId);
        } catch (CacheAccessException e) {
            // Cache invalidation failure is non-critical - log and continue
            logger.warn("Failed to invalidate cache for team {}, but database update succeeded", teamId, e);
        } catch (Exception e) {
            logger.warn("Unexpected error while invalidating cache for team {}", teamId, e);
        }
    }
}