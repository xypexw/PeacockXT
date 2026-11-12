package com.github.peacockxt.service.provider;

import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.service.RedisRetryManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
@AllArgsConstructor
public class MembershipProvider {

    private final MembershipRepository membershipRepository;
    private final MembershipRedisDao membershipRedisDao;
    private final RedisRetryManager redisRetryManager;

    public boolean hasMembership(Long membershipId) {
        return getMembership(membershipId) != null;
    }

    public Membership getMembership(Long membershipId) {
        if (membershipId == null) return null;
        Membership membership = getMembershipFromCache(membershipId);
        if (membership != null) return membership;
        try {

            membership = membershipRepository.getMembershipByMembershipId(membershipId);
            if (membership != null) {
                saveMembershipToCache(membership);
            }
            return membership;

        } catch (Exception e) {
            log.error("Failed to get membership from database for ID: {}", membershipId, e);
            throw new DatabaseException("Error querying Membership from DB", e);
        }
    }

    private Membership getMembershipFromCache(Long membershipId) {
        try {
            return membershipRedisDao.getMembership(membershipId);
        } catch (Exception e) {
            log.warn("Cache read failed for membership ID {}: {}", membershipId, e.getMessage());
            return null;
        }
    }

    private void saveMembershipToCache(Membership membership) {
        try {
            membershipRedisDao.saveMembershipInfo(membership);
        } catch (Exception e) {
            log.warn("Failed to write membership to cache. Delegating to Retry Manager", e);
            redisRetryManager.execute(() -> membershipRedisDao.saveMembershipInfo(membership));
        }
    }



    @Transactional
    public void deleteBulkMembership(List<Long> membershipIds , Long teamId){
        if(membershipIds.isEmpty()) return;
        List<Membership> memberships = membershipRepository.getMembershipByMembershipIdIn(membershipIds);
        membershipRepository.deleteMembershipsByMembershipIdIn(membershipIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                membershipRedisDao.removeMembershipInfoBatch(membershipIds);
                membershipRedisDao.removeMembershipIdInManyMembersInTeam(memberships);
                membershipRedisDao.deleteMembershipIndexTeamKey(teamId);
            }
        });
    }

    public List<Long> getMembershipIdsFromTeam(Long teamId) {
        Set<Long> teamMembershipIdsInCache = membershipRedisDao.getAllMembershipIdsInTeam(teamId);
        if(teamMembershipIdsInCache != null && !teamMembershipIdsInCache.isEmpty()){
            return new ArrayList<>(teamMembershipIdsInCache);
        }
        return membershipRepository.findMembershipIdsByTeamId(teamId);
    }

}