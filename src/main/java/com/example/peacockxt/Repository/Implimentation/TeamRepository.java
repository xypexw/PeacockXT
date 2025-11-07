package com.example.peacockxt.Repository.Implimentation;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.SystemModule.Membership;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
@Repository
public interface TeamRepository extends JpaRepository<Team,String> {
    Team findTeamByTeamId(String teamId);
    List<Team> findTeamsByMemberships(java.util.List<Membership> memberships);

    Team getTeamByTeamId(String teamId);
}
