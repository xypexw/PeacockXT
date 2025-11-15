package com.example.peacockxt.Service.ChannelModule;

import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import org.springframework.stereotype.Service;

@Service
public class HelperChannelService {
    private final TeamRepository teamRepository;

    public HelperChannelService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }


    public String getTeamKeyPrefix(String teamId) {
        String teamKeyPrefix = "TEAM::";
        return teamId + teamKeyPrefix;
    }

    public String getChannelKeyPrefix(String channelId) {
        String channelKeyPrefix = "CHANNEL::";
        return channelId + channelKeyPrefix;
    }

    public Team getTeam(String teamId) {
        try{
            return teamRepository.getTeamByTeamId(teamId);
        } catch (Exception e) {
            throw new DatabaseException("Database Fail at Get Team" , e);
        }
    }

}
