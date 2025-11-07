package com.example.peacockxt.Controller.DTO;
import java.util.*;
import com.example.peacockxt.Models.GroupModule.Team;
import lombok.Data;

@Data
public class LoginResponse {
    public String jwtToken;
    public String userId;
    public String urlLink;
}
