package com.example.peacockxt.Repository.Implimentation;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.SystemModule.Membership;
import com.example.peacockxt.Models.SystemModule.MembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {
    @Query(value = "Select m.team from Membership m where m.user.userId = :userId")
    List<Team> findTeamsByUserId(@Param("userId")String userId);
    @Modifying
    @Query(value = "delete Membership m where m.user.userId = :userId and m.team.teamId = :teamId")
    void deleteMemberShipUserIdAndTeamId(@Param("userId")String userId , @Param("teamId")String teamId);

}