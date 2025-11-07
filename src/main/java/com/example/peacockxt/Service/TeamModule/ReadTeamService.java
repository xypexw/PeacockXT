package com.example.peacockxt.Service.TeamModule;
import java.util.*;

import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.SystemModule.Membership;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.TeamRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import com.example.peacockxt.Repository.MembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ReadTeamService {
    private final MembershipRepository membershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    public ReadTeamService(MembershipRepository membershipRepository, TeamRepository teamRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<Team> getTeamByUserId(String userId){
        List<Membership> memberships =  membershipRepository.findAllByUserId(userId);
        return teamRepository.findTeamsByMemberships(memberships);
    }

    @Transactional
    public void joinTeam(String userId , String teamId){
        User user = userRepository.getUserByUserId(userId);
        Team team = teamRepository.getTeamByTeamId(teamId);
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setTeam(team);
        membershipRepository.save(membership);
    }

}
