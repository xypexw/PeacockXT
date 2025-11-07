package com.example.peacockxt.Repository;

import com.example.peacockxt.Models.SystemModule.Membership;
import com.example.peacockxt.Models.SystemModule.MembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {
    List<Membership> findAllByUserId(String userId);
}