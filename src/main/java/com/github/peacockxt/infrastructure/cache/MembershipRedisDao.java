package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class MembershipRedisDao {

    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisTemplate<String, CacheMembership> membershipRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void saveMembershipInfo(CacheMembership membership) {
        if (membership == null || membership.getMembershipId() == null) return;
        redisGuard.safeWrite(() -> membershipRedisTemplate.opsForValue().set(
                        RedisKey.MBS_INFO.build(membership.getMembershipId()),
                        membership,
                        addJitter(RedisKey.MBS_INFO.getTtl())
                ),
                "saveMembershipInfo for ID: " + membership.getMembershipId()
        );
    }

    public void addMembershipToTeam(Long membershipId, Long teamId) {
        if (membershipId == null || teamId == null) return;
        redisGuard.safeWrite(
                () -> {
                    String key = RedisKey.MBS_IN_TEAMS.build(teamId);
                    longRedisTemplate.opsForSet().add(key, membershipId);
                    longRedisTemplate.expire(key, addJitter(RedisKey.MBS_IN_TEAMS.getTtl()));
                },
                "addMembershipToTeam [Membership: " + membershipId + " -> Team: " + teamId + "]"
        );
    }

    public void addMembershipToUser(Long membershipId, Long userId) {
        if (membershipId == null || userId == null) return;
        redisGuard.safeWrite(
                () -> {
                    String key = RedisKey.USER_MBS.build(userId);
                    longRedisTemplate.opsForSet().add(key, membershipId);
                    longRedisTemplate.expire(key, addJitter(RedisKey.USER_MBS.getTtl()));
                },
                "addMembershipToUser [Membership: " + membershipId + " -> User: " + userId + "]"
        );
    }

    public Set<Long> getAllMembershipIdsInTeam(Long teamId) {
        if (teamId == null) return Collections.emptySet();
        return redisGuard.safeRead(
                () -> {
                    Set<Long> result = longRedisTemplate.opsForSet().members(RedisKey.MBS_IN_TEAMS.build(teamId));
                    return result == null ? Collections.emptySet() : result;
                },
                Collections.emptySet(),
                "getAllMembershipIdsInTeam for Team: " + teamId
        );
    }

    public Set<Long> getAllMembershipIdsFromMember(Long userId) {
        if (userId == null) return Collections.emptySet();
        return redisGuard.safeRead(
                () -> {
                    Set<Long> result = longRedisTemplate.opsForSet().members(RedisKey.USER_MBS.build(userId));
                    return result == null ? Collections.emptySet() : result;
                },
                Collections.emptySet(),
                "getAllMembershipIdsFromMember for User: " + userId
        );
    }

    public void removeMembershipFromTeam(Long membershipId, Long teamId) {
        if (membershipId == null || teamId == null) return;
        redisGuard.safeInvalidate(
                () -> longRedisTemplate.opsForSet().remove(RedisKey.MBS_IN_TEAMS.build(teamId), membershipId),
                "removeMembershipFromTeam [Membership: " + membershipId + " from Team: " + teamId + "]"
        );
    }

    public void removeMembershipFromMember(Long membershipId, Long userId) {
        if (membershipId == null || userId == null) return;
        redisGuard.safeInvalidate(
                () -> longRedisTemplate.opsForSet().remove(RedisKey.USER_MBS.build(userId), membershipId),
                "removeMembershipFromMember [Membership: " + membershipId + " from User: " + userId + "]"
        );
    }

    public boolean hasMembershipInfo(Long membershipId) {
        if (membershipId == null) return false;
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> membershipRedisTemplate.hasKey(RedisKey.MBS_INFO.build(membershipId)),
                false,
                "hasMembershipInfo for ID: " + membershipId
        ));
    }

    public boolean hasMembershipIndexTeamKey(Long teamId) {
        if (teamId == null) return false;
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> longRedisTemplate.hasKey(RedisKey.MBS_IN_TEAMS.build(teamId)),
                false,
                "hasMembershipInTeamKey for ID: " + teamId
        ));
    }

    public boolean hasMembershipIndexUserKey(Long userId) {
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> longRedisTemplate.hasKey(RedisKey.USER_MBS.build(userId)),
                false,
                "hasUserMembershipsKey for ID: " + userId
        ));
    }

    // ================= Batch Operations =================

    public List<CacheMembership> getMembershipBatch(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(
                () -> {
                    List<String> keys = membershipIds.stream().map(RedisKey.MBS_INFO::build).toList();
                    List<CacheMembership> result = membershipRedisTemplate.opsForValue().multiGet(keys);
                    return (result == null || result.isEmpty()) ? Collections.emptyList() : result;
                },
                Collections.emptyList(),
                "getMembershipBatch for " + membershipIds.size() + " IDs"
        );
    }

    public CacheMembership getMembership(Long membershipId) {
        if (membershipId == null) return null;
        return redisGuard.safeRead(
                () -> membershipRedisTemplate.opsForValue().get(RedisKey.MBS_INFO.build(membershipId)),
                null,
                "getMembership for ID: " + membershipId
        );
    }

    public void removeMembershipIdInManyMembersInTeam(List<CacheMembership> memberships) {
        if (memberships == null || memberships.isEmpty()) return;

        redisGuard.safeInvalidate(
                () -> {
                    Map<String, Long> keyToMembershipIdMap = new HashMap<>();
                    for (CacheMembership membership : memberships) {
                        if (membership != null && membership.getUserId() != null && membership.getMembershipId() != null) {
                            String key = RedisKey.USER_MBS.build(membership.getUserId());
                            keyToMembershipIdMap.put(key, membership.getMembershipId());
                        }
                    }
                    redisBatch.multiSetRemovePairs(longRedisTemplate, keyToMembershipIdMap);
                },
                "removeMembershipIdInManyMembersInTeam for " + memberships.size() + " memberships"
        );
    }

    public void removeMembershipInfo(Long membershipId) {
        if (membershipId == null) return;
        redisGuard.safeInvalidate(
                () -> membershipRedisTemplate.delete(RedisKey.MBS_INFO.build(membershipId)),
                "removeMembershipInfo for ID: " + membershipId
        );
    }

    public void saveAllMembershipIdsToTeam(List<Long> membershipIds, Long teamId) {
        if (teamId == null || membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    String key = RedisKey.MBS_IN_TEAMS.build(teamId);
                    Long[] idsArray = membershipIds.toArray(new Long[0]);
                    longRedisTemplate.opsForSet().add(key, idsArray);
                    longRedisTemplate.expire(key, addJitter(RedisKey.MBS_IN_TEAMS.getTtl()));
                },
                "saveAllMembershipIdsToTeam for Team: " + teamId + " (" + membershipIds.size() + " IDs)"
        );
    }

    public void saveAllMembershipIdsToUser(List<Long> membershipIds, Long userId) {
        if (userId == null || membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    String key = RedisKey.USER_MBS.build(userId);
                    Long[] idsArray = membershipIds.toArray(new Long[0]);
                    longRedisTemplate.opsForSet().add(key, idsArray);
                    longRedisTemplate.expire(key, addJitter(RedisKey.USER_MBS.getTtl()));
                },
                "saveAllMembershipIdsToUser for User: " + userId + " (" + membershipIds.size() + " IDs)"
        );
    }

    public void removeMembershipInfoBatch(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeInvalidate(
                () -> {
                    List<String> keys = membershipIds.stream().map(RedisKey.MBS_INFO::build).toList();
                    membershipRedisTemplate.delete(keys);
                },
                "removeMembershipInfoBatch for " + membershipIds.size() + " Memberships"
        );
    }

    public void deleteMembershipIndexTeamKey(Long teamId) {
        if (teamId == null) return;

        redisGuard.safeInvalidate(
                () -> longRedisTemplate.delete(RedisKey.MBS_IN_TEAMS.build(teamId)),
                "deleteMembershipIndexTeamKey for Team: " + teamId
        );
    }

    public void saveMembershipInfoBatch(List<CacheMembership> memberships) {
        if (memberships == null || memberships.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Map<String, CacheMembership> membershipMap = new HashMap<>();
                    for (CacheMembership membership : memberships) {
                        if (membership != null && membership.getMembershipId() != null) {
                            membershipMap.put(RedisKey.MBS_INFO.build(membership.getMembershipId()), membership);
                        }
                    }
                    redisBatch.multiSetWithTtl(membershipRedisTemplate, membershipMap, addJitter(RedisKey.MBS_INFO.getTtl()));
                },
                "saveMembershipInfoBatch for " + memberships.size() + " Memberships"
        );
    }
}
