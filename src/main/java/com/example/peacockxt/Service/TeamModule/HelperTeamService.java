package com.example.peacockxt.Service.TeamModule;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HelperTeamService {

    public String getTeamKey(String teamId){
        String teamKeyPrefix = "TEAM:";
        return teamKeyPrefix + teamId;
    }

}
