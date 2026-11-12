package com.github.peacockxt.service.membership;

import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamPermission;
import com.github.peacockxt.service.provider.UserProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@AllArgsConstructor
@Slf4j
public class DeleteMembershipService {
    private final TeamPermission teamPermission;
    private final MembershipProvider membershipProviderService;
    private final MembershipRepository membershipRepository;
    private final MembershipRedisDao membershipRedisDao;
    private final UserProvider userProvider;

    @Transactional
    public void deleteMembership(Long updatedMembershipId , Long updaterMembershipId , Long updaterId) {
        if(!membershipProviderService.hasMembership(updaterMembershipId))
            throw new BusinessLogicException("Membership id not found");
        if(!membershipProviderService.hasMembership(updatedMembershipId))
            throw new BusinessLogicException("Membership id not found");
        if(!userProvider.hasUser(updaterId))
            throw new BusinessLogicException("User id not found");
        boolean isAdmin = teamPermission.isTeamAdmin(updaterMembershipId,updaterId);
        boolean isUser = updatedMembershipId.equals(updaterMembershipId);
        if (!isAdmin && !isUser)
            throw new BusinessLogicException("User not allowed to update membership");
        Membership membership = membershipProviderService.getMembership(updatedMembershipId);
        Long membershipId = membership.getMembershipId();
        Long userId = membership.getUserId();
        Long teamId = membership.getTeamId();
        deleteMembershipToDatabase(membership);


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteMembershipInCache(membershipId,teamId,userId);
            }
        });
    }

    private void deleteMembershipToDatabase(Membership membership) {
        membershipRepository.delete(membership);
    }

    private void deleteMembershipInCache(Long membershipId ,  Long teamId , Long userId) {
        membershipRedisDao.removeMembershipInfo(membershipId);
        if(membershipRedisDao.hasMembershipIndexTeamKey(teamId)){
            membershipRedisDao.removeMembershipFromTeam(membershipId, teamId);
        }
        if(membershipRedisDao.hasMembershipIndexUserKey(userId)){
            membershipRedisDao.removeMembershipFromMember(membershipId, userId);
        }
    }

}
