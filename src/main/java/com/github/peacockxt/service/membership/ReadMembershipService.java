package com.github.peacockxt.service.membership;

import com.github.peacockxt.dto.response.MembershipResponse;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
import com.github.peacockxt.service.RedisRetryManager;
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
    private final RedisRetryManager redisRetryManager;
    // find the certain membership
    public MembershipResponse getSingleMembership(Long userId , Long teamId) {

        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }

        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }
        List<Long> membershipIds = getMembershipIdsOfUser(userId);
        List<Membership> memberships = getMembershipsByIds(membershipIds);
        Membership membership = memberships.stream()
                .filter(m -> Objects.equals(m.getTeamId(), teamId) && Objects.equals(m.getUserId(), userId))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("User doesn't belong to this team"));
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
        List<Membership> memberships = getMembershipsByIds(membershipIds);

        return buildResponses(memberships);
    }

    public List<MembershipResponse> getTeamMemberships(Long teamId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }
        List<Long> membershipIds = getMembershipIdsInTeam(teamId);
        List<Membership> memberships = getMembershipsByIds(membershipIds);

        return buildResponses(memberships);
    }

    // ==========================================
    // 2. FETCH INDEX LIST LOGIC (CACHE ASIDE)
    // ==========================================

    private List<Long> getMembershipIdsOfUser(Long userId) {
        Set<Long> cachedIds = membershipRedisDao.getAllMembershipIdsFromMember(userId);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return new ArrayList<>(cachedIds);
        }

        List<Long> dbIds = membershipRepository.findMembershipIdsByUserId(userId);

        // Delegate caching to RetryManager to run in the background, return results immediately for faster response
        membershipRedisDao.saveAllMembershipIdsToUser(dbIds, userId);
        return dbIds;
    }

    private List<Long> getMembershipIdsInTeam(Long teamId) {
        Set<Long> cachedIds = membershipRedisDao.getAllMembershipIdsInTeam(teamId);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return new ArrayList<>(cachedIds);
        }

        List<Long> dbIds = membershipRepository.findMembershipIdsByTeamId(teamId);
        redisRetryManager.execute(() -> membershipRedisDao.saveAllMembershipIdsToTeam(dbIds, teamId));
        return dbIds;
    }

    // ==========================================
    // 3. CORE: PARTIAL CACHE ASIDE LOGIC
    // ==========================================

    private List<Membership> getMembershipsByIds(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return Collections.emptyList();

        // Step 1: Scan Cache
        List<Membership> cachedMemberships = membershipRedisDao.getMembershipBatch(membershipIds);

        // Total Cache Miss -> Fallback to Database
        if (cachedMemberships == null || cachedMemberships.isEmpty()) {
            return fetchAndCacheMissingMemberships(membershipIds);
        }

        // Step 2: Collect missing IDs from the Cache (Partial Miss)
        List<Long> missingIds = new ArrayList<>();
        for (int i = 0; i < membershipIds.size(); i++) {
            if (cachedMemberships.get(i) == null) {
                missingIds.add(membershipIds.get(i));
            }
        }

        // 100% Cache Hit -> Return immediately
        if (missingIds.isEmpty()) return cachedMemberships;

        // Step 3: Fetch the missing data from the DB and put it into a Map
        List<Membership> missingMemberships = fetchAndCacheMissingMemberships(missingIds);
        Map<Long, Membership> dbDataMap = new HashMap<>();
        for (Membership m : missingMemberships) {
            dbDataMap.put(m.getMembershipId(), m);
        }

        // Step 4: Merge Cache and DB data while maintaining the original index order
        List<Membership> result = new ArrayList<>(membershipIds.size());
        for (int i = 0; i < membershipIds.size(); i++) {
            Membership cached = cachedMemberships.get(i);

            // If present in Cache, use the cached data
            if (cached != null) {
                result.add(cached);
            }
            // If it was a Cache miss, retrieve it from the DB map we just queried
            else {
                Membership fromDb = dbDataMap.get(membershipIds.get(i));
                // Skip Ghost IDs (in case the ID was deleted from DB but lingered in the index)
                if (fromDb != null) {
                    result.add(fromDb);
                }
            }
        }

        return result;
    }

    // Utility function: Fetch data from DB and push to Queue for background caching
    private List<Membership> fetchAndCacheMissingMemberships(List<Long> missingIds) {
        List<Membership> dbMemberships = membershipRepository.findMembershipsByMembershipIds(missingIds);
        if (!dbMemberships.isEmpty()) {
            redisRetryManager.execute(() -> membershipRedisDao.saveMembershipInfoBatch(dbMemberships));
        }
        return dbMemberships;
    }

    private List<MembershipResponse> buildResponses(List<Membership> memberships) {
        return memberships.stream().map(m -> MembershipResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(m.getUserId())
                .role(m.getRole())
                .build()
        ).toList();
    }



}