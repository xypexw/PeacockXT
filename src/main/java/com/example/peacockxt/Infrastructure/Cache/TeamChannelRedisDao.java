package com.example.peacockxt.Infrastructure.Cache;

import com.example.peacockxt.Models.Channel;
import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Infrastructure.Constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.DefaultTypedTuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TeamChannelRedisDao {

    private final RedisTemplate<String, Channel> channelRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, Team> teamRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    // ==========================================
    // 1. TEAM CHANNELS INDEX (ZSET)
    // ==========================================

    public void addChannelToTeam(Long teamId, Long channelId, Long displayOrder) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForZSet()
                        .add(RedisKey.TEAM_CHANNELS.build(teamId), channelId, displayOrder),
                "addChannelToTeam: teamId=" + teamId + ", channelId=" + channelId
        );
    }

    public void removeChannelIndexFromTeam(Long teamId, Long channelId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.opsForZSet()
                        .remove(RedisKey.TEAM_CHANNELS.build(teamId), channelId),
                "removeChannelIndexFromTeam: teamId=" + teamId + ", channelId=" + channelId
        );
    }

    public void removeAllChannelIndexesInTeam(Long teamId) {
        redisGuard.safeWrite(
                () -> longRedisTemplate.delete(RedisKey.TEAM_CHANNELS.build(teamId)),
                "removeAllChannelIndexesInTeam: teamId=" + teamId
        );
    }

    public Set<Long> getChannelIdsFromTeam(Long teamId) {
        return redisGuard.safeRead(
                () -> {
                    Set<Long> result = longRedisTemplate.opsForZSet()
                            .range(RedisKey.TEAM_CHANNELS.build(teamId), 0, -1);
                    return result == null ? Collections.emptySet() : result;
                },
                Collections.emptySet(),
                "getChannelIdsFromTeam: teamId=" + teamId
        );
    }

    public void saveChannelIndexesBatch(Long teamId, List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Set<ZSetOperations.TypedTuple<Long>> tuples = new HashSet<>();
            for (Long channelId : channelIds) {
                ZSetOperations.TypedTuple<Long> tuple = new DefaultTypedTuple<>(channelId, channelId.doubleValue());
                tuples.add(tuple);
            }
            longRedisTemplate.opsForZSet().add(RedisKey.TEAM_CHANNELS.build(teamId), tuples);
        }, "saveChannelIndexesBatch: teamId=" + teamId + ", size=" + channelIds.size());
    }

    // ==========================================
    // 2. TEAM INFO
    // ==========================================

    public Team getTeamInfo(Long teamId) {
        return redisGuard.safeRead(
                () -> teamRedisTemplate.opsForValue().get(RedisKey.TEAM_INFO.build(teamId)),
                null,
                "getTeamInfo: teamId=" + teamId
        );
    }

    public Boolean hasTeamInfo(Long teamId) {
        return redisGuard.safeRead(
                () -> teamRedisTemplate.hasKey(RedisKey.TEAM_INFO.build(teamId)),
                false,
                "hasTeamInfo: teamId=" + teamId
        );
    }

    public List<Team> getAllTeamInfo(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = new ArrayList<>();
            for (Long teamId : teamIds) {
                keys.add(RedisKey.TEAM_INFO.build(teamId));
            }
            List<Team> teams = teamRedisTemplate.opsForValue().multiGet(keys);
            return teams == null ? Collections.emptyList() : teams;
        }, Collections.emptyList(), "getAllTeamInfo: size=" + teamIds.size());
    }

    public void deleteTeamInfo(Long teamId) {
        redisGuard.safeWrite(
                () -> teamRedisTemplate.delete(RedisKey.TEAM_INFO.build(teamId)),
                "deleteTeamInfo: teamId=" + teamId
        );
    }

    public void saveTeamInfo(Team team) {
        if (team == null) return;

        redisGuard.safeWrite(
                () -> teamRedisTemplate.opsForValue().set(RedisKey.TEAM_INFO.build(team.getTeamId()), team),
                "saveTeamInfo: teamId=" + team.getTeamId()
        );
    }

    public void saveTeamInfoBatch(List<Team> teams) {
        if (teams == null || teams.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, Team> teamMap = new HashMap<>();
            for (Team team : teams) {
                teamMap.put(RedisKey.TEAM_INFO.build(team.getTeamId()), team);
            }
            redisBatch.multiSetWithTtl(teamRedisTemplate, teamMap, addJitter(RedisKey.TEAM_INFO.getTtl()));
        }, "saveTeamInfoBatch: size=" + teams.size());
    }

    // ==========================================
    // 3. CHANNEL INFO
    // ==========================================

    public void saveChannelInfo(Long channelId, Channel channel) {
        redisGuard.safeWrite(() -> {
            channelRedisTemplate.opsForValue().set(
                    RedisKey.CHANNEL_INFO.build(channelId),
                    channel,
                    addJitter(RedisKey.CHANNEL_INFO.getTtl())
            );
        }, "saveChannelInfo: channelId=" + channelId);
    }

    public Channel getChannelInfo(Long channelId) {
        return redisGuard.safeRead(
                () -> channelRedisTemplate.opsForValue().get(RedisKey.CHANNEL_INFO.build(channelId)),
                null,
                "getChannelInfo: channelId=" + channelId
        );
    }

    public boolean hasChannelInfo(Long channelId) {
        return redisGuard.safeRead(
                () -> Boolean.TRUE.equals(channelRedisTemplate.hasKey(RedisKey.CHANNEL_INFO.build(channelId))),
                false,
                "hasChannelInfo: channelId=" + channelId
        );
    }

    public void deleteChannelInfo(Long channelId) {
        redisGuard.safeWrite(
                () -> channelRedisTemplate.delete(RedisKey.CHANNEL_INFO.build(channelId)),
                "deleteChannelInfo: channelId=" + channelId
        );
    }

    public void multiSetChannelInfo(List<Channel> channels) {
        if (channels == null || channels.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, Channel> channelMap = new HashMap<>();
            for (Channel channel : channels) {
                channelMap.put(
                        RedisKey.CHANNEL_INFO.build(channel.getChannelId()),
                        channel
                );
            }
            redisBatch.multiSetWithTtl(
                    channelRedisTemplate,
                    channelMap,
                    addJitter(RedisKey.CHANNEL_INFO.getTtl())
            );
        }, "multiSetChannelInfo: size=" + channels.size());
    }

    public List<Channel> multiGetChannelInfo(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = new ArrayList<>();
            for (Long channelId : channelIds) {
                keys.add(RedisKey.CHANNEL_INFO.build(channelId));
            }
            List<Channel> channels = channelRedisTemplate.opsForValue().multiGet(keys);
            return channels == null ? Collections.emptyList() : channels;
        }, Collections.emptyList(), "multiGetChannelInfo: size=" + channelIds.size());
    }
}