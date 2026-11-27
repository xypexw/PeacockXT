package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.MembershipRepository;
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

    public boolean hasMembership(Long membershipId) {
        return getMembership(membershipId) != null;
    }

    public CacheMembership getMembership(Long membershipId) {
        if (membershipId == null) return null;
        CacheMembership cached = membershipRedisDao.getMembership(membershipId);
        if (cached != null) return cached;
        try {
            Membership membership = membershipRepository.getMembershipByMembershipId(membershipId);
            if (membership == null) {
                return null;
            }
            CacheMembership cacheMembership = toCacheMembership(membership);
            membershipRedisDao.saveMembershipInfo(cacheMembership);
            return cacheMembership;
        } catch (Exception e) {
            log.error("Failed to get membership from database for ID: {}", membershipId, e);
            throw new DatabaseException("Error querying Membership from DB", e);
        }
    }

    public CacheMembership getMembershipByUserAndTeam(Long userId, Long teamId) {
        if (userId == null || teamId == null) return null;
        try {
            Membership membership = membershipRepository.findByUserIdAndTeamId(userId, teamId).orElse(null);
            if (membership == null) {
                return null;
            }
            CacheMembership cacheMembership = toCacheMembership(membership);
            membershipRedisDao.saveMembershipInfo(cacheMembership);
            return cacheMembership;
        } catch (Exception e) {
            log.error("Failed to get membership by userId={} and teamId={}", userId, teamId, e);
            throw new DatabaseException("Error querying Membership from DB", e);
        }
    }

    @Transactional
    public void deleteBulkMembership(List<Long> membershipIds, Long teamId) {
        if (membershipIds == null || membershipIds.isEmpty()) return;
        List<Membership> memberships = membershipRepository.getMembershipByMembershipIdIn(membershipIds);
        membershipRepository.deleteMembershipsByMembershipIdIn(membershipIds);
        List<CacheMembership> cacheMemberships = new ArrayList<>(memberships.size());
        for (Membership m : memberships) {
            cacheMemberships.add(toCacheMembership(m));
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                membershipRedisDao.removeMembershipInfoBatch(membershipIds);
                membershipRedisDao.removeMembershipIdInManyMembersInTeam(cacheMemberships);
                membershipRedisDao.deleteMembershipIndexTeamKey(teamId);
            }
        });
    }

    public List<Long> getMembershipIdsFromTeam(Long teamId) {
        if (teamId == null) return List.of();
        Set<Long> teamMembershipIdsInCache = membershipRedisDao.getAllMembershipIdsInTeam(teamId);
        if (teamMembershipIdsInCache != null && !teamMembershipIdsInCache.isEmpty()) {
            return new ArrayList<>(teamMembershipIdsInCache);
        }
        List<Long> dbIds = membershipRepository.findMembershipIdsByTeamId(teamId);
        if (dbIds != null && !dbIds.isEmpty()) {
            membershipRedisDao.saveAllMembershipIdsToTeam(dbIds, teamId);
        }
        return dbIds;
    }

    private CacheMembership toCacheMembership(Membership membership) {
        return CacheMembership.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUserId())
                .teamId(membership.getTeamId())
                .role(membership.getRole())
                .joinedAt(membership.getJoinedAt())
                .build();
    }
}
