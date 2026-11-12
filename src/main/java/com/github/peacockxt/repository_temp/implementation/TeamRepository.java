package com.github.peacockxt.repository_temp.implementation;

import com.github.peacockxt.models_temp.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface TeamRepository extends JpaRepository<Team,Long> {
    Team getTeamByTeamId(Long teamId);
    @Query("select t from Team t where t.teamId in :teamIds")
    List<Team> getTeamsByTeamIds(@Param("teamIds") List<Long> teamIds);

}
