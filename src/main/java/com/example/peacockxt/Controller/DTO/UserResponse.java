package com.example.peacockxt.Controller.DTO;

import com.example.peacockxt.Models.GroupModule.Team;
import lombok.Data;
import java.util.List;

@Data
public class UserResponse {
    private List<Team> teams;
    private String urlLink;
}
