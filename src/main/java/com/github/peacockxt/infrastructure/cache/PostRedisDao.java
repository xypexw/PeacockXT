package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CachePost;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostRedisDao {

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

    private final RedisTemplate<String, CachePost> postRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    private static String formatId(Long id) {
        return String.format("%019d", id);
    }

    public void savePost(Long teamId, CachePost post) {
        if (teamId == null || post == null || post.getPostId() == null) return;

        redisGuard.safeWrite(
                () -> {
                    Long postId = post.getPostId();

                    postRedisTemplate.opsForValue().set(
                            RedisKey.POST_DATA.build(postId),
                            post,
                            addJitter(RedisKey.POST_DATA.getTtl())
                    );

                    String indexKey = RedisKey.TEAM_POST_INDEX.build(teamId);
                    Duration indexTtl = addJitter(RedisKey.TEAM_POST_INDEX.getTtl());
                    stringRedisTemplate.execute(
                            ADD_IF_EXISTS_SCRIPT,
                            Collections.singletonList(indexKey),
                            formatId(postId),
                            String.valueOf(-(MAX_INDEX_SIZE + 1)),
                            String.valueOf(indexTtl.getSeconds())
                    );
                },
                "savePost [PostId: " + post.getPostId() + "] for Team: " + teamId
        );
    }

    public void savePostBatch(List<CachePost> posts) {
        if (posts == null || posts.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Map<String, CachePost> postMap = new HashMap<>();
                    for (CachePost post : posts) {
                        if (post != null && post.getPostId() != null) {
                            postMap.put(RedisKey.POST_DATA.build(post.getPostId()), post);
                        }
                    }
                    redisBatch.multiSetWithTtl(
                            postRedisTemplate,
                            postMap,
                            addJitter(RedisKey.POST_DATA.getTtl())
                    );
                },
                "savePostBatch for " + posts.size() + " posts"
        );
    }

    public CachePost getPostData(Long postId) {
        if (postId == null) return null;
        return redisGuard.safeRead(
                () -> postRedisTemplate.opsForValue().get(RedisKey.POST_DATA.build(postId)),
                null,
                "getPostData for PostId: " + postId
        );
    }

    public List<CachePost> getPostsBatch(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(
                () -> {
                    List<String> keys = postIds.stream()
                            .map(RedisKey.POST_DATA::build)
                            .collect(Collectors.toList());

                    List<CachePost> posts = postRedisTemplate.opsForValue().multiGet(keys);
                    if (posts == null || posts.isEmpty()) return Collections.emptyList();
                    return posts;
                },
                Collections.emptyList(),
                "getPostsBatch for " + postIds.size() + " posts"
        );
    }

    public Set<Long> getPostIdsBeforeCursor(Long teamId, Long cursorPostId, int limit) {
        if (teamId == null || limit <= 0) return Collections.emptySet();

        return redisGuard.safeRead(
                () -> {
                    String indexKey = RedisKey.TEAM_POST_INDEX.build(teamId);
                    Set<String> raw;
                    if (cursorPostId == null) {
                        raw = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
                    } else {
                        raw = stringRedisTemplate.opsForZSet().reverseRangeByLex(
                                indexKey,
                                Range.leftUnbounded(Range.Bound.exclusive(formatId(cursorPostId))),
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
                "getPostIdsBeforeCursor for Team: " + teamId + " Limit: " + limit
        );
    }

    public void deletePostData(Long postId) {
        if (postId == null) return;
        redisGuard.safeInvalidate(
                () -> postRedisTemplate.delete(RedisKey.POST_DATA.build(postId)),
                "deletePostData for PostId: " + postId
        );
    }

    public void deletePostIndex(Long teamId, Long postId) {
        if (teamId == null || postId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.opsForZSet().remove(RedisKey.TEAM_POST_INDEX.build(teamId), formatId(postId)),
                "remove postId : " + postId + " for Team: " + teamId
        );
    }

    public void deleteTeamPostIndex(Long teamId) {
        if (teamId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.delete(RedisKey.TEAM_POST_INDEX.build(teamId)),
                "deleteTeamPostIndex for Team: " + teamId
        );
    }

    public void savePostIndexes(List<Long> postIds, Long teamId) {
        if (teamId == null || postIds == null || postIds.isEmpty()) {
            return;
        }

        redisGuard.safeWrite(() -> {
            String key = RedisKey.TEAM_POST_INDEX.build(teamId);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(postIds.size());
            for (Long postId : postIds) {
                if (postId != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(postId), 0.0));
                }
            }

            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
                stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(key, addJitter(RedisKey.TEAM_POST_INDEX.getTtl()));
            }
        }, "savePostIndexes for Team: " + teamId);
    }
}
