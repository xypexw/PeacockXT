package com.github.peacockxt.service.membership;

import com.github.peacockxt.dto.response.MembershipResponse;
import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReadMembershipService {
    private final MembershipRepository membershipRepository;
    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final MembershipRedisDao membershipRedisDao;
    private final MembershipProvider membershipProvider;

    public MembershipResponse getSingleMembership(Long userId, Long teamId) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        CacheMembership membership = membershipProvider.getMembershipByUserAndTeam(userId, teamId);
        if (membership == null) {
            throw new BusinessLogicException("User doesn't belong to this team");
        }

        MembershipResponse membershipResponse = new MembershipResponse();
        membershipResponse.setMembershipId(membership.getMembershipId());
        membershipResponse.setRole(membership.getRole());
        membershipResponse.setUserId(membership.getUserId());
        return membershipResponse;
    }

    public List<MembershipResponse> getMemberMemberships(Long userId) {
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }
        List<Long> membershipIds = getMembershipIdsOfUser(userId);
        List<CacheMembership> memberships = getMembershipsByIds(membershipIds);
        return buildResponses(memberships);
    }

    public List<MembershipResponse> getTeamMemberships(Long teamId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }
        List<Long> membershipIds = getMembershipIdsInTeam(teamId);
        List<CacheMembership> memberships = getMembershipsByIds(membershipIds);
        return buildResponses(memberships);
    }

    private List<Long> getMembershipIdsOfUser(Long userId) {
        Set<Long> cachedIds = membershipRedisDao.getAllMembershipIdsFromMember(userId);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return new ArrayList<>(cachedIds);
        }

        List<Long> dbIds = membershipRepository.findMembershipIdsByUserId(userId);
        if (dbIds != null && !dbIds.isEmpty()) {
            membershipRedisDao.saveAllMembershipIdsToUser(dbIds, userId);
        }
        return dbIds != null ? dbIds : Collections.emptyList();
    }

    private List<Long> getMembershipIdsInTeam(Long teamId) {
        Set<Long> cachedIds = membershipRedisDao.getAllMembershipIdsInTeam(teamId);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return new ArrayList<>(cachedIds);
        }

        List<Long> dbIds = membershipRepository.findMembershipIdsByTeamId(teamId);
        if (dbIds != null && !dbIds.isEmpty()) {
            membershipRedisDao.saveAllMembershipIdsToTeam(dbIds, teamId);
        }
        return dbIds != null ? dbIds : Collections.emptyList();
    }

    private List<CacheMembership> getMembershipsByIds(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return Collections.emptyList();

        List<CacheMembership> cachedMemberships = membershipRedisDao.getMembershipBatch(membershipIds);
        Map<Long, CacheMembership> pool = new HashMap<>(membershipIds.size());
        if (cachedMemberships != null) {
            for (CacheMembership m : cachedMemberships) {
                if (m != null) {
                    pool.put(m.getMembershipId(), m);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : membershipIds) {
            if (!pool.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Membership> dbMemberships = membershipRepository.findMembershipsByMembershipIds(missingIds);
            List<CacheMembership> toCache = new ArrayList<>(dbMemberships.size());
            for (Membership entity : dbMemberships) {
                CacheMembership cacheMembership = toCacheMembership(entity);
                pool.put(cacheMembership.getMembershipId(), cacheMembership);
                toCache.add(cacheMembership);
            }
            if (!toCache.isEmpty()) {
                membershipRedisDao.saveMembershipInfoBatch(toCache);
            }
        }

        List<CacheMembership> result = new ArrayList<>(membershipIds.size());
        for (Long id : membershipIds) {
            CacheMembership m = pool.get(id);
            if (m != null) {
                result.add(m);
            }
        }
        return result;
    }

    private List<MembershipResponse> buildResponses(List<CacheMembership> memberships) {
        return memberships.stream().map(m -> MembershipResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(m.getUserId())
                .role(m.getRole())
                .build()
        ).toList();
    }

    private CacheMembership toCacheMembership(Membership entity) {
        return CacheMembership.builder()
                .membershipId(entity.getMembershipId())
                .userId(entity.getUserId())
                .teamId(entity.getTeamId())
                .role(entity.getRole())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}
