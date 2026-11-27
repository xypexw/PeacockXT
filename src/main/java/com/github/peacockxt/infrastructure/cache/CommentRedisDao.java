package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheComment;
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
public class CommentRedisDao {

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

    private final RedisTemplate<String, CacheComment> commentRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(1800);
        return baseTtl.plusSeconds(jitterSeconds);
    }

    private static String formatId(Long id) {
        return String.format("%019d", id);
    }

    public void saveComment(Long postId, CacheComment comment) {
        if (postId == null || comment == null || comment.getCommentId() == null) return;

        redisGuard.safeWrite(() -> {
            Long commentId = comment.getCommentId();

            String dataKey = RedisKey.COMMENT_DATA.build(commentId);
            Duration ttl = addJitter(RedisKey.COMMENT_DATA.getTtl());
            commentRedisTemplate.opsForValue().set(dataKey, comment, ttl);

            String indexKey = RedisKey.POST_COMMENT_INDEX.build(postId);
            Duration indexTtl = addJitter(RedisKey.POST_COMMENT_INDEX.getTtl());
            stringRedisTemplate.execute(
                    ADD_IF_EXISTS_SCRIPT,
                    Collections.singletonList(indexKey),
                    formatId(commentId),
                    String.valueOf(-(MAX_INDEX_SIZE + 1)),
                    String.valueOf(indexTtl.getSeconds())
            );
        }, "saveComment: postId=" + postId + ", commentId=" + comment.getCommentId());
    }

    public void saveCommentBatch(List<CacheComment> comments) {
        if (comments == null || comments.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, CacheComment> commentMap = new HashMap<>();
            for (CacheComment comment : comments) {
                if (comment != null && comment.getCommentId() != null) {
                    commentMap.put(RedisKey.COMMENT_DATA.build(comment.getCommentId()), comment);
                }
            }
            redisBatch.multiSetWithTtl(
                    commentRedisTemplate,
                    commentMap,
                    addJitter(RedisKey.COMMENT_DATA.getTtl())
            );
        }, "saveCommentBatch: size=" + comments.size());
    }

    public void saveCommentData(CacheComment comment) {
        if (comment == null || comment.getCommentId() == null) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.COMMENT_DATA.build(comment.getCommentId());
            Duration ttl = addJitter(RedisKey.COMMENT_DATA.getTtl());
            commentRedisTemplate.opsForValue().set(key, comment, ttl);
        }, "saveCommentData: commentId=" + comment.getCommentId());
    }

    public void saveCommentIndexesBatch(Long postId, List<Long> commentIds) {
        if (postId == null || commentIds == null || commentIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.POST_COMMENT_INDEX.build(postId);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(commentIds.size());
            for (Long commentId : commentIds) {
                if (commentId != null) {
                    tuples.add(new DefaultTypedTuple<>(formatId(commentId), 0.0));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
                stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
                stringRedisTemplate.expire(key, addJitter(RedisKey.POST_COMMENT_INDEX.getTtl()));
            }
        }, "saveCommentIndexesBatch: postId=" + postId + ", size=" + commentIds.size());
    }

    public CacheComment getCommentData(Long commentId) {
        if (commentId == null) return null;
        return redisGuard.safeRead(() -> {
            String dataKey = RedisKey.COMMENT_DATA.build(commentId);
            return commentRedisTemplate.opsForValue().get(dataKey);
        }, null, "getCommentData: commentId=" + commentId);
    }

    public List<CacheComment> getCommentsBatch(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = commentIds.stream()
                    .map(RedisKey.COMMENT_DATA::build)
                    .collect(Collectors.toList());

            List<CacheComment> comments = commentRedisTemplate.opsForValue().multiGet(keys);
            return (comments == null || comments.isEmpty()) ? Collections.emptyList() : comments;
        }, Collections.emptyList(), "getCommentsBatch: size=" + commentIds.size());
    }

    public Set<Long> getCommentIdsBeforeCursor(Long postId, Long cursorCommentId, int limit) {
        if (postId == null || limit <= 0) return Collections.emptySet();

        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.POST_COMMENT_INDEX.build(postId);
            Set<String> raw;
            if (cursorCommentId == null) {
                raw = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
            } else {
                raw = stringRedisTemplate.opsForZSet().reverseRangeByLex(
                        indexKey,
                        Range.leftUnbounded(Range.Bound.exclusive(formatId(cursorCommentId))),
                        Limit.limit().count(limit)
                );
            }
            if (raw == null || raw.isEmpty()) return Collections.emptySet();
            Set<Long> result = new LinkedHashSet<>(raw.size());
            for (String s : raw) {
                result.add(Long.parseLong(s));
            }
            return result;
        }, Collections.emptySet(), "getCommentIdsBeforeCursor: postId=" + postId + ", cursor=" + cursorCommentId);
    }

    public void deleteCommentData(Long commentId) {
        if (commentId == null) return;
        redisGuard.safeInvalidate(
                () -> commentRedisTemplate.delete(RedisKey.COMMENT_DATA.build(commentId)),
                "deleteCommentData: commentId=" + commentId
        );
    }

    public void deleteCommentIndex(Long postId, Long commentId) {
        if (postId == null || commentId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.opsForZSet().remove(RedisKey.POST_COMMENT_INDEX.build(postId), formatId(commentId)),
                "deleteCommentIndex: postId=" + postId + ", commentId=" + commentId
        );
    }

    public void deletePostCommentIndex(Long postId) {
        if (postId == null) return;
        redisGuard.safeInvalidate(
                () -> stringRedisTemplate.delete(RedisKey.POST_COMMENT_INDEX.build(postId)),
                "deletePostCommentIndex: postId=" + postId
        );
    }
}
