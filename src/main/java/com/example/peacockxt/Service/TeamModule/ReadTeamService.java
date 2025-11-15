package com.example.peacockxt.Service.TeamModule;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadTeamService {

    private static final Logger logger = LoggerFactory.getLogger(ReadTeamService.class);
    private final RedisTemplate<String, Team> redisTemplate;
    private final TeamRepository teamRepository;

    public ReadTeamService(RedisTemplate<String, Team> redisTemplate, TeamRepository teamRepository){
        this.redisTemplate = redisTemplate;
        this.teamRepository = teamRepository;
    }

    public Team getSingleTeam(String teamId) {
        try{
            // Trước tiên kiểm tra cache
            Team cachedTeam = redisTemplate.opsForValue().get(teamId);
            if (cachedTeam != null) {
                logger.info("Cache Hit : team {} found in cache", teamId);
                return cachedTeam;
            }

            // Nếu không có trong cache, lấy từ database
            Team team = teamRepository.getTeamByTeamId(teamId);
            if (team != null) {
                redisTemplate.opsForValue().set(teamId, team); // lưu vào cache
                logger.info("Cache MISS : team {} not found in cache, loaded from DB", teamId);
            }
            return team;
        }
        catch(DatabaseException e){
            throw new DatabaseException("Error accessing database", e);
        }
        catch(CacheAccessException e){
            throw new CacheAccessException("Error accessing cache", e);
        }
    }

    private List<Team> getTeamInDatabase(List<String> teamIdList){
        try{
            return teamRepository.getTeamsByTeamIds(teamIdList);
        }
        catch(Exception e){
            throw new DatabaseException("Error accessing database", e);
        }
    }

    private List<Team> getTeamInCache(List<String> teamIdList){
        try{
            List<Team> cachedTeams = new ArrayList<>();
            for (String teamId : teamIdList) {
                Team team = redisTemplate.opsForValue().get(teamId);
                if (team != null) {
                    cachedTeams.add(team);
                }
            }
            return cachedTeams;
        }
        catch(Exception e){
            throw new CacheAccessException("Error accessing cache", e);
        }
    }

    public List<Team> getAllTeams(List<String> teamIdList) {
        try{
            List<Team> teamsResult = new ArrayList<>();

            // Lấy những team có trong cache trước
            try{
                teamsResult = getTeamInCache(teamIdList);
            }
            catch(CacheAccessException e){
                logger.warn("Cache access failed: {}", e.getMessage());
            }

            // Tìm team chưa có trong cache
            List<Team> finalTeamsResult = teamsResult;
            List<String> missingIds = teamIdList.stream()
                    .filter(id -> finalTeamsResult.stream().noneMatch(t -> t.getTeamId().equals(id)))
                    .collect(Collectors.toList());

            // Lấy team còn thiếu từ database
            List<Team> teamsFromDb = getTeamInDatabase(missingIds);

            // Lưu các team mới lấy từ DB vào cache
            for (Team team : teamsFromDb) {
                try {
                    redisTemplate.opsForValue().set(team.getTeamId(), team);
                } catch(Exception e) {
                    logger.warn("Failed to cache team {}: {}", team.getTeamId(), e.getMessage());
                }
            }

            // Kết hợp kết quả và trả về
            List<Team> allTeams = new ArrayList<>(teamsResult);
            allTeams.addAll(teamsFromDb);
            return allTeams;
        }
        catch(DatabaseException e){
            throw new BusinessLogicException("Failed to retrieve teams", e);
        }
    }
}
