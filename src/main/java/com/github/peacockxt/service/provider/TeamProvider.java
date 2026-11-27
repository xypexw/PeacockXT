package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheTeam;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.TeamRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TeamProvider {

    private final TeamRepository teamRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;

    public boolean hasTeam(Long teamId) {
        return getTeam(teamId) != null;
    }

    public CacheTeam getTeam(Long teamId) {
        if (teamId == null) return null;
        CacheTeam cached = teamChannelRedisDao.getTeamInfo(teamId);
        if (cached != null) return cached;

        try {
            Team team = teamRepository.getTeamByTeamId(teamId);
            if (team == null) return null;

            CacheTeam cacheTeam = CacheTeam.builder()
                    .teamId(team.getTeamId())
                    .name(team.getName())
                    .description(team.getDescription())
                    .status(team.getStatus())
                    .type(team.getType())
                    .createAt(team.getCreateAt())
                    .updateAt(team.getUpdateAt())
                    .createBy(team.getCreateBy())
                    .updateBy(team.getUpdateBy())
                    .build();
            teamChannelRedisDao.saveTeamInfo(cacheTeam);
            return cacheTeam;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Team from database", e);
        }
    }
}
