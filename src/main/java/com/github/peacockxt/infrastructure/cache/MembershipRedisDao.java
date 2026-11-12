package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.Membership;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class MembershipRedisDao {

    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisTemplate<String, Membership> membershipRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void saveMembershipInfo(Membership membership) {
        redisGuard.safeWrite(() -> membershipRedisTemplate.opsForValue().set(
                        RedisKey.MBS_INFO.build(membership.getMembershipId()),
                        membership,
                        addJitter(RedisKey.MBS_INFO.getTtl())
                ),
                "saveMembershipInfo for ID: " + membership.getMembershipId()
        );
    }

    public void addMembershipToTeam(Long membershipId, Long teamId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForSet().add(RedisKey.MBS_IN_TEAMS.build(teamId), membershipId),
                "addMembershipToTeam [Membership: " + membershipId + " -> Team: " + teamId + "]"
        );
    }

    public void addMembershipToUser(Long membershipId, Long userId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForSet().add(RedisKey.USER_MBS.build(userId), membershipId),
                "addMembershipToUser [Membership: " + membershipId + " -> User: " + userId + "]"
        );
    }

    public Set<Long> getAllMembershipIdsInTeam(Long teamId) {
        return redisGuard.safeRead(
                () -> longRedisTemplate.opsForSet().members(RedisKey.MBS_IN_TEAMS.build(teamId)),
                Collections.emptySet(),
                "getAllMembershipIdsInTeam for Team: " + teamId
        );
    }

    public Set<Long> getAllMembershipIdsFromMember(Long userId) {
        return redisGuard.safeRead(
                () -> longRedisTemplate.opsForSet().members(RedisKey.USER_MBS.build(userId)),
                Collections.emptySet(),
                "getAllMembershipIdsFromMember for User: " + userId
        );
    }

    public void removeMembershipFromTeam(Long membershipId, Long teamId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForSet().remove(RedisKey.MBS_IN_TEAMS.build(teamId), membershipId),
                "removeMembershipFromTeam [Membership: " + membershipId + " from Team: " + teamId + "]"
        );
    }

    public void removeMembershipFromMember(Long membershipId, Long userId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForSet().remove(RedisKey.USER_MBS.build(userId), membershipId),
                "removeMembershipFromMember [Membership: " + membershipId + " from User: " + userId + "]"
        );
    }

    public boolean hasMembershipInfo(Long membershipId) {
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> membershipRedisTemplate.hasKey(RedisKey.MBS_INFO.build(membershipId)),
                false, // Default to false if Redis is down
                "hasMembershipInfo for ID: " + membershipId
        ));
    }

    public boolean hasMembershipIndexTeamKey(Long teamId) {
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> membershipRedisTemplate.hasKey(RedisKey.MBS_IN_TEAMS.build(teamId)),
                false, // Default to false if Redis is down
                "hasMembershipInTeamKey for ID: " + teamId
        ));
    }

    public boolean hasMembershipIndexUserKey(Long userId) {
        return Boolean.TRUE.equals(redisGuard.safeRead(
                () -> membershipRedisTemplate.hasKey(RedisKey.USER_MBS.build(userId)),
                false, // Default to false if Redis is down
                "hasUserMembershipsKey for ID: " + userId
        ));
    }


    // ================= Batch Operations =================

    public List<Membership> getMembershipBatch(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(
                () -> {
                    List<String> keys = membershipIds.stream().map(RedisKey.MBS_INFO::build).toList();
                    return membershipRedisTemplate.opsForValue().multiGet(keys);
                },
                Collections.emptyList(),
                "getMembershipBatch for " + membershipIds.size() + " IDs"
        );
    }

    public Membership getMembership(Long membershipId) {
        return redisGuard.safeRead(
                () -> membershipRedisTemplate.opsForValue().get(RedisKey.MBS_INFO.build(membershipId)),
                null,
                "getMembership for ID: " + membershipId
        );
    }

    public void removeMembershipIdInManyMembersInTeam(List<Membership> memberships) {
        if (memberships == null || memberships.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Map<String, Long> keyToMembershipIdMap = new HashMap<>();
                    for (Membership membership : memberships) {
                        String key = RedisKey.USER_MBS.build(membership.getUserId());
                        keyToMembershipIdMap.put(key, membership.getMembershipId());
                    }
                    redisBatch.multiSetRemovePairs(longRedisTemplate, keyToMembershipIdMap);
                },
                "removeMembershipIdInManyMembersInTeam for " + memberships.size() + " memberships"
        );
    }

    public void removeMembershipInfo(Long membershipId) {
        redisGuard.safeWrite(
                () -> membershipRedisTemplate.delete(RedisKey.MBS_INFO.build(membershipId)),
                "removeMembershipInfo for ID: " + membershipId
        );
    }

    public void saveAllMembershipIdsToTeam(List<Long> membershipIds, Long teamId) {
        if (membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Long[] idsArray = membershipIds.toArray(new Long[0]);
                    longRedisTemplate.opsForSet().add(RedisKey.MBS_IN_TEAMS.build(teamId), idsArray);
                },
                "saveAllMembershipIdsToTeam for Team: " + teamId + " (" + membershipIds.size() + " IDs)"
        );
    }

    public void saveAllMembershipIdsToUser(List<Long> membershipIds, Long userId) {
        if (membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Long[] idsArray = membershipIds.toArray(new Long[0]);
                    longRedisTemplate.opsForSet().add(RedisKey.USER_MBS.build(userId), idsArray);
                },
                "saveAllMembershipIdsToUser for User: " + userId + " (" + membershipIds.size() + " IDs)"
        );
    }

    public void removeMembershipInfoBatch(List<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    List<String> keys = membershipIds.stream().map(RedisKey.MBS_INFO::build).toList();
                    membershipRedisTemplate.delete(keys);
                },
                "removeMembershipInfoBatch for " + membershipIds.size() + " Memberships"
        );
    }

    public void deleteMembershipIndexTeamKey( Long teamId){
        if(teamId == null) return;

        redisGuard.safeWrite(
                () -> membershipRedisTemplate.delete(RedisKey.MBS_IN_TEAMS.build(teamId)),
        "deleteMembershipIndexTeamKey for Team: " + teamId
        );
    }

    public void saveMembershipInfoBatch(List<Membership> memberships) {
        if (memberships == null || memberships.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Map<String, Membership> membershipMap = new HashMap<>();
                    for (Membership membership : memberships) {
                        membershipMap.put(RedisKey.MBS_INFO.build(membership.getMembershipId()), membership);
                    }
                    redisBatch.multiSetWithTtl(membershipRedisTemplate, membershipMap, addJitter(RedisKey.MBS_INFO.getTtl()));
                },
                "saveMembershipInfoBatch for " + memberships.size() + " Memberships"
        );
    }
}