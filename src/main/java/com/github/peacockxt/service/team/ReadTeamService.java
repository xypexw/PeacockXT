package com.github.peacockxt.service.team;

import com.github.peacockxt.dto.response.TeamResponse;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheTeam;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.TeamRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReadTeamService {
    private static final int MAX_BULK_TEAMS = 100;

    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;

    public List<TeamResponse> getTeamForBulkAPI(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (teamIds.size() > MAX_BULK_TEAMS) {
            throw new BusinessLogicException("Maximum " + MAX_BULK_TEAMS + " teamIds per request");
        }

        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(teamIds));
        List<CacheTeam> teamsInCache = teamChannelRedisDao.getAllTeamInfo(uniqueIds);
        Map<Long, CacheTeam> teamMap = new HashMap<>(uniqueIds.size());
        if (teamsInCache != null) {
            for (CacheTeam t : teamsInCache) {
                if (t != null) {
                    teamMap.put(t.getTeamId(), t);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : uniqueIds) {
            if (!teamMap.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Team> teamsFromDatabase = teamRepository.getTeamsByTeamIds(missingIds);
            List<CacheTeam> toCache = new ArrayList<>(teamsFromDatabase.size());
            for (Team entity : teamsFromDatabase) {
                CacheTeam cacheTeam = toCacheTeam(entity);
                teamMap.put(cacheTeam.getTeamId(), cacheTeam);
                toCache.add(cacheTeam);
            }
            if (!toCache.isEmpty()) {
                teamChannelRedisDao.saveTeamInfoBatch(toCache);
            }
        }

        List<CacheTeam> result = new ArrayList<>(uniqueIds.size());
        for (Long id : uniqueIds) {
            CacheTeam team = teamMap.get(id);
            if (team != null) {
                result.add(team);
            }
        }

        return responses(result);
    }

    List<TeamResponse> responses(List<CacheTeam> teams) {
        List<TeamResponse> responses = new ArrayList<>();
        for (CacheTeam team : teams) {
            if (team != null && !"DELETED".equals(team.getStatus())) {
                responses.add(toResponse(team));
            }
        }
        return responses;
    }

    private TeamResponse toResponse(CacheTeam team) {
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .status(team.getStatus())
                .type(team.getType())
                .build();
    }

    private CacheTeam toCacheTeam(Team entity) {
        return CacheTeam.builder()
                .teamId(entity.getTeamId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .type(entity.getType())
                .createAt(entity.getCreateAt())
                .updateAt(entity.getUpdateAt())
                .createBy(entity.getCreateBy())
                .updateBy(entity.getUpdateBy())
                .build();
    }
}
