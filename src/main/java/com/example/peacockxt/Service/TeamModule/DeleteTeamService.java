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

import java.util.Optional;

@Service
public class DeleteTeamService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteTeamService.class);
    private final TeamCacheDeleterService teamCacheDeleterService;
    private final TeamRepository teamRepository;

    public DeleteTeamService(TeamCacheDeleterService teamCacheDeleterService, TeamRepository teamRepository) {
        this.teamCacheDeleterService = teamCacheDeleterService;
        this.teamRepository = teamRepository;
    }


    private void deleteTeamFromDatabase(String teamId) {
        try {
            Optional<Team> teamOptional = teamRepository.findById(teamId);
            if (teamOptional.isEmpty()) {
                logger.warn("Database Delete: team {} not found in database", teamId);
                return;
            }
            teamRepository.delete(teamOptional.get());
            logger.info("Database Delete: team {} removed from database", teamId);

        } catch (Exception e) {
            logger.error("Database Delete Failed: error removing team {} from database", teamId, e);
            throw new DatabaseException("Failed to delete team from database", e);
        }
    }

    @Transactional
    public void deleteTeam(String teamId) {
        try {
            logger.info("Start delete process for team {}", teamId);
            teamCacheDeleterService.deleteTeamFromCache(teamId);
            deleteTeamFromDatabase(teamId);
            logger.info("Delete process completed for team {}", teamId);
        }
        catch (CacheAccessException | DatabaseException e) {
            logger.error("Team Delete Failed: error occurred while deleting team {}", teamId, e);
            throw new BusinessLogicException("Delete team failed due to internal error", e);
        }
    }
}
