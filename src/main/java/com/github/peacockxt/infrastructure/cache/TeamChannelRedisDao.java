package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheChannel;
import com.github.peacockxt.infrastructure.cache.models.CacheTeam;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TeamChannelRedisDao {

    private static final int MAX_INDEX_SIZE = 500;

    private static final DefaultRedisScript<Long> ADD_IF_EXISTS_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
            "  redis.call('ZADD', KEYS[1], 0, ARGV[1]); " +
            "  redis.call('ZREMRANGEBYRANK', KEYS[1], 0, tonumber(ARGV[2])); " +
            "  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3])); " +
            "  return 1; " +
            "end; " +
            "return 0;",
            Long.class
    );

    private final RedisTemplate<String, CacheChannel> channelRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, CacheTeam> teamRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    private static String formatId(Long id) {
        return String.format("%019d", id);
    }

    // ==========================================
    // 1. TEAM CHANNELS INDEX (ZSET)
    // ==========================================

    public void addChannelToTeam(Long teamId, Long channelId) {
        if (teamId == null || channelId == null) return;
        redisGuard.safeWrite(
                () -> {
                    String indexKey = RedisKey.TEAM_CHANNELS.build(teamId);
                    Duration indexTtl = addJitter(RedisKey.TEAM_CHANNELS.getTtl());
                    stringRedisTemplate.execute(
                            ADD_IF_EXISTS_SCRIPT,
                            Collections.singletonList(indexKey),
                            formatId(channelId),
                            String.valueOf(-(MAX_INDEX_SIZE + 1)),
                            String.valueOf(indexTtl.getSeconds())
                    );
                },
                "addChannelToTeam: teamId=" + teamId + ", channelId=" + channelId
        );
    }

    public void removeChannelIndexFromTeam(Long teamId, Long channelId) {
        if (teamId == null || channelId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.opsForZSet()
                        .remove(RedisKey.TEAM_CHANNELS.build(teamId), formatId(channelId)),
                "removeChannelIndexFromTeam: teamId=" + teamId + ", channelId=" + channelId
        );
    }

    public void removeAllChannelIndexesInTeam(Long teamId) {
        if (teamId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.delete(RedisKey.TEAM_CHANNELS.build(teamId)),
                "removeAllChannelIndexesInTeam: teamId=" + teamId
        );
    }

    public Set<Long> getChannelIdsBeforeCursor(Long teamId, Long cursorId, int limit) {
        if (teamId == null || limit <= 0) return Collections.emptySet();
        return redisGuard.safeRead(
                () -> {
                    String indexKey = RedisKey.TEAM_CHANNELS.build(teamId);
                    Set<String> raw;
                    if (cursorId == null) {
                        raw = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
                    } else {
                        raw = stringRedisTemplate.opsForZSet().reverseRangeByLex(
                                indexKey,
                                Range.leftUnbounded(Range.Bound.exclusive(formatId(cursorId))),
                                Limit.limit().count(limit)
                        );
                    }
                    if (raw == null || raw.isEmpty()) return Collections.emptySet();
                    Set<Long> result = new LinkedHashSet<>(raw.size());
                    for (String s : raw) {
                        result.add(Long.parseLong(s));
                    }
                    return result;
                },
                Collections.emptySet(),
                "getChannelIdsBeforeCursor: teamId=" + teamId + ", cursor=" + cursorId
        );
    }

    public void saveChannelIndexesBatch(Long teamId, List<Long> channelIds) {
        if (teamId == null || channelIds == null || channelIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.TEAM_CHANNELS.build(teamId);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(channelIds.size());
            for (Long channelId : channelIds) {
                if (channelId != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(channelId), 0.0));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
                stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(key, addJitter(RedisKey.TEAM_CHANNELS.getTtl()));
            }
        }, "saveChannelIndexesBatch: teamId=" + teamId + ", size=" + channelIds.size());
    }

    // ==========================================
    // 2. TEAM INFO
    // ==========================================

    public CacheTeam getTeamInfo(Long teamId) {
        if (teamId == null) return null;
        return redisGuard.safeRead(
                () -> teamRedisTemplate.opsForValue().get(RedisKey.TEAM_INFO.build(teamId)),
                null,
                "getTeamInfo: teamId=" + teamId
        );
    }

    public Boolean hasTeamInfo(Long teamId) {
        if (teamId == null) return false;
        return redisGuard.safeRead(
                () -> teamRedisTemplate.hasKey(RedisKey.TEAM_INFO.build(teamId)),
                false,
                "hasTeamInfo: teamId=" + teamId
        );
    }

    public List<CacheTeam> getAllTeamInfo(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = new ArrayList<>(teamIds.size());
            for (Long teamId : teamIds) {
                keys.add(RedisKey.TEAM_INFO.build(teamId));
            }
            List<CacheTeam> teams = teamRedisTemplate.opsForValue().multiGet(keys);
            return teams == null ? Collections.emptyList() : teams;
        }, Collections.emptyList(), "getAllTeamInfo: size=" + teamIds.size());
    }

    public void deleteTeamInfo(Long teamId) {
        if (teamId == null) return;
        redisGuard.safeInvalidate(
                () -> teamRedisTemplate.delete(RedisKey.TEAM_INFO.build(teamId)),
                "deleteTeamInfo: teamId=" + teamId
        );
    }

    public void saveTeamInfo(CacheTeam team) {
        if (team == null || team.getTeamId() == null) return;

        redisGuard.safeWrite(
                () -> teamRedisTemplate.opsForValue().set(
                        RedisKey.TEAM_INFO.build(team.getTeamId()),
                        team,
                        addJitter(RedisKey.TEAM_INFO.getTtl())
                ),
                "saveTeamInfo: teamId=" + team.getTeamId()
        );
    }

    public void saveTeamInfoBatch(List<CacheTeam> teams) {
        if (teams == null || teams.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, CacheTeam> teamMap = new HashMap<>();
            for (CacheTeam team : teams) {
                if (team != null && team.getTeamId() != null) {
                    teamMap.put(RedisKey.TEAM_INFO.build(team.getTeamId()), team);
                }
            }
            redisBatch.multiSetWithTtl(teamRedisTemplate, teamMap, addJitter(RedisKey.TEAM_INFO.getTtl()));
        }, "saveTeamInfoBatch: size=" + teams.size());
    }

    // ==========================================
    // 3. CHANNEL INFO
    // ==========================================

    public void saveChannelInfo(Long channelId, CacheChannel channel) {
        if (channelId == null || channel == null) return;
        redisGuard.safeWrite(() -> channelRedisTemplate.opsForValue().set(
                RedisKey.CHANNEL_INFO.build(channelId),
                channel,
                addJitter(RedisKey.CHANNEL_INFO.getTtl())
        ), "saveChannelInfo: channelId=" + channelId);
    }

    public CacheChannel getChannelInfo(Long channelId) {
        if (channelId == null) return null;
        return redisGuard.safeRead(
                () -> channelRedisTemplate.opsForValue().get(RedisKey.CHANNEL_INFO.build(channelId)),
                null,
                "getChannelInfo: channelId=" + channelId
        );
    }

    public boolean hasChannelInfo(Long channelId) {
        if (channelId == null) return false;
        return redisGuard.safeRead(
                () -> channelRedisTemplate.hasKey(RedisKey.CHANNEL_INFO.build(channelId)),
                false,
                "hasChannelInfo: channelId=" + channelId
        );
    }

    public void deleteChannelInfo(Long channelId) {
        if (channelId == null) return;
        redisGuard.safeInvalidate(
                () -> channelRedisTemplate.delete(RedisKey.CHANNEL_INFO.build(channelId)),
                "deleteChannelInfo: channelId=" + channelId
        );
    }

    public void multiSetChannelInfo(List<CacheChannel> channels) {
        if (channels == null || channels.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, CacheChannel> channelMap = new HashMap<>();
            for (CacheChannel channel : channels) {
                if (channel != null && channel.getChannelId() != null) {
                    channelMap.put(
                            RedisKey.CHANNEL_INFO.build(channel.getChannelId()),
                            channel
                    );
                }
            }
            redisBatch.multiSetWithTtl(
                    channelRedisTemplate,
                    channelMap,
                    addJitter(RedisKey.CHANNEL_INFO.getTtl())
            );
        }, "multiSetChannelInfo: size=" + channels.size());
    }

    public List<CacheChannel> multiGetChannelInfo(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = new ArrayList<>(channelIds.size());
            for (Long channelId : channelIds) {
                keys.add(RedisKey.CHANNEL_INFO.build(channelId));
            }
            List<CacheChannel> channels = channelRedisTemplate.opsForValue().multiGet(keys);
            return channels == null ? Collections.emptyList() : channels;
        }, Collections.emptyList(), "multiGetChannelInfo: size=" + channelIds.size());
    }
}
