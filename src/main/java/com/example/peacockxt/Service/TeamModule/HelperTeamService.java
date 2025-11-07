package com.example.peacockxt.Service.TeamModule;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HelperTeamService {
    private final String teamKeyPrefix = "TEAM:";
    private final String userKeyPrefix = "USER:";

    public String getTeamKey(String teamId){
        return teamKeyPrefix + teamId;
    }

    public String getUserKey(String teamId){
        return userKeyPrefix + teamId;
    }

}
