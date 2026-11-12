package com.github.peacockxt.service.membership;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.models.User;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamPermission;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@AllArgsConstructor
@Slf4j
public class UpdateMembershipService {
    private final TeamPermission teamPermission;
    private final MembershipProvider membershipProviderService;
    private final MembershipRepository membershipRepository;
    private final MembershipRedisDao membershipRedisDao;

    @Transactional
    public void updateMembership(Long updatedMembershipId , Long updaterMembershipId , String role , Long updaterId) {
        if(!membershipProviderService.hasMembership(updaterMembershipId))
            throw new BusinessLogicException("Membership id not found");
        if(!membershipProviderService.hasMembership(updatedMembershipId))
            throw new BusinessLogicException("Membership id not found");
        if(!teamPermission.isTeamAdmin(updaterMembershipId,updaterId))
            throw new BusinessLogicException("User not allowed to update membership");
        Membership membership = membershipProviderService.getMembership(updatedMembershipId);
        membership.setRole(role);
        Team dummyTeam = new Team();
        dummyTeam.setTeamId(membership.getTeamId());
        membership.setTeam(dummyTeam); // get Dummy for not being NULL
        User dummyUser = new User();
        dummyUser.setUserId(membership.getUserId());
        membership.setUser(dummyUser);
        saveMembershipToDatabase(membership);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteMembershipInCache(membership);
            }
        });
    }

    private void saveMembershipToDatabase(Membership membership) {
        membershipRepository.save(membership);
    }

    private void deleteMembershipInCache(Membership membership) {
        membershipRedisDao.removeMembershipInfo(membership.getMembershipId());
    }

}
