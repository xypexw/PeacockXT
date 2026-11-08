package com.example.peacockxt.Repository.Imlpementation;
import com.example.peacockxt.Models.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership,Long> {

    @Query("SELECT m.membershipId FROM Membership m WHERE m.user.userId = :userId")
    List<Long> findMembershipIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT m.membershipId FROM Membership m WHERE m.team.teamId = :teamId")
    List<Long> findMembershipIdsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT m FROM Membership m WHERE m.membershipId IN :membershipIds")
    List<Membership> findMembershipsByMembershipIds(@Param("membershipIds") List<Long> membershipIds);
    Membership getMembershipByMembershipId(Long membershipId);
    void deleteMembershipsByMembershipIdIn(Collection<Long> membershipIds);
    List<Membership> getMembershipByMembershipIdIn(Collection<Long> membershipIds);
}