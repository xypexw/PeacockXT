package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.Comment;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentRedisDao {

    // Retain up to the 500 most recent comments per post
    private static final int MAX_INDEX_SIZE = 500;

    private final RedisTemplate<String, Comment> commentRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    // --- HELPER: Add Jitter to TTL (Random from 0 to 30 minutes) ---
    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(1800);
        return baseTtl.plusSeconds(jitterSeconds);
    }

    // ==========================================
    // 1. SAVE / UPDATE COMMENT (WRITE -> safeWrite)
    // ==========================================
    public void saveComment(Long postId, Comment comment) {
        redisGuard.safeWrite(() -> {
            Long commentId = comment.getCommentId();

            // 1. Save Data
            String dataKey = RedisKey.COMMENT_DATA.build(commentId);
            Duration ttl = addJitter(RedisKey.COMMENT_DATA.getTtl());
            commentRedisTemplate.opsForValue().set(dataKey, comment, ttl);

            // 2. Save Index (Attach to post's ZSet index)
            String indexKey = RedisKey.POST_COMMENT_INDEX.build(postId);
            longRedisTemplate.opsForZSet().add(indexKey, commentId, commentId.doubleValue());

            // 3. Trim Index to keep only the latest 500 comments
            longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
        }, "saveComment: postId=" + postId + ", commentId=" + comment.getCommentId());
    }

    public void saveCommentBatch(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            Map<String, Comment> commentMap = new HashMap<>();
            for (Comment comment : comments) {
                commentMap.put(RedisKey.COMMENT_DATA.build(comment.getCommentId()), comment);
            }
            redisBatch.multiSetWithTtl(
                    commentRedisTemplate,
                    commentMap,
                    addJitter(RedisKey.COMMENT_DATA.getTtl())
            );
        }, "saveCommentBatch: size=" + comments.size());
    }

    public void saveCommentData(Comment comment) {
        if (comment == null) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.COMMENT_DATA.build(comment.getCommentId());
            Duration ttl = addJitter(RedisKey.COMMENT_DATA.getTtl());
            commentRedisTemplate.opsForValue().set(key, comment, ttl);
        }, "saveCommentData: commentId=" + comment.getCommentId());
    }

    public void saveCommentIndexesBatch(Long postId, List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.POST_COMMENT_INDEX.build(postId);
            Set<ZSetOperations.TypedTuple<Long>> tuples = new HashSet<>();
            for (Long commentId : commentIds) {
                tuples.add(new DefaultTypedTuple<>(commentId, commentId.doubleValue()));
            }
            longRedisTemplate.opsForZSet().add(key, tuples);
            longRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INDEX_SIZE + 1));
        }, "saveCommentIndexesBatch: postId=" + postId + ", size=" + commentIds.size());
    }

    // ==========================================
    // 2. GET COMMENT - SINGLE & BATCH (READ -> safeRead)
    // ==========================================
    public Comment getCommentData(Long commentId) {
        return redisGuard.safeRead(() -> {
            String dataKey = RedisKey.COMMENT_DATA.build(commentId);
            return commentRedisTemplate.opsForValue().get(dataKey);
        }, null, "getCommentData: commentId=" + commentId);
    }

    public List<Comment> getCommentsBatch(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(() -> {
            List<String> keys = commentIds.stream()
                    .map(RedisKey.COMMENT_DATA::build)
                    .collect(Collectors.toList());

            List<Comment> comments = commentRedisTemplate.opsForValue().multiGet(keys);
            return (comments == null || comments.isEmpty()) ? Collections.emptyList() : comments;
        }, Collections.emptyList(), "getCommentsBatch: size=" + commentIds.size());
    }

    // ==========================================
    // 3. CURSOR - PAGINATION (READ -> safeRead)
    // ==========================================
    public Set<Long> getCommentIdsBeforeCursor(Long postId, Long cursorCommentId, int limit) {
        return redisGuard.safeRead(() -> {
            String indexKey = RedisKey.POST_COMMENT_INDEX.build(postId);

            if (cursorCommentId == null) {
                // Initial page load: retrieve the most recent comment IDs
                Set<Long> result = longRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
                return result == null ? Collections.emptySet() : result;
            } else {
                // Subsequent page loads: fetch comments older than cursor
                Set<Long> result = longRedisTemplate.opsForZSet().reverseRangeByScore(
                        indexKey, 0, cursorCommentId - 1, 0, limit
                );
                return result == null ? Collections.emptySet() : result;
            }
        }, Collections.emptySet(), "getCommentIdsBeforeCursor: postId=" + postId + ", cursor=" + cursorCommentId);
    }

    // ==========================================
    // 4. DELETE COMMENT (WRITE/DELETE -> safeWrite)
    // ==========================================
    public void deleteCommentData(Long commentId) {
        redisGuard.safeWrite(
                () -> commentRedisTemplate.delete(RedisKey.COMMENT_DATA.build(commentId)),
                "deleteCommentData: commentId=" + commentId
        );
    }
}