package com.example.peacockxt.Repository.Implimentation;
import com.example.peacockxt.Models.GroupModule.Team;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
@Repository
public interface TeamRepository extends JpaRepository<Team,String> {
    Team findTeamByTeamId(String teamId);
    Team getTeamByTeamId(String teamId);
    @Query("SELECT t FROM Team t WHERE t.teamId IN :teamIds")
    List<Team> getTeamsByTeamIds(@Param("teamIds") List<String> teamIdList);
}
