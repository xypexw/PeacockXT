package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.Post;
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
public class PostRedisDao {

    private static final int MAX_INDEX_SIZE = 500;

    private final RedisTemplate<String, Post> postRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void savePost(Long teamId, Post post) {
        redisGuard.safeWrite(
                () -> {
                    Long postId = post.getPostId();

                    postRedisTemplate.opsForValue().set(
                            RedisKey.POST_DATA.build(postId),
                            post,
                            addJitter(RedisKey.POST_DATA.getTtl())
                    );

                    String indexKey = RedisKey.TEAM_POST_INDEX.build(teamId);
                    longRedisTemplate.opsForZSet().add(indexKey, postId, postId.doubleValue());
                    longRedisTemplate.opsForZSet().removeRange(indexKey, 0, -(MAX_INDEX_SIZE + 1));
                },
                "savePost [PostId: " + post.getPostId() + "] for Team: " + teamId
        );
    }

    public void savePostBatch(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;

        redisGuard.safeWrite(
                () -> {
                    Map<String, Post> postMap = new HashMap<>();
                    for (Post post : posts) {
                        postMap.put(RedisKey.POST_DATA.build(post.getPostId()), post);
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

    public Post getPostData(Long postId) {
        return redisGuard.safeRead(
                () -> postRedisTemplate.opsForValue().get(RedisKey.POST_DATA.build(postId)),
                null,
                "getPostData for PostId: " + postId
        );
    }

    public List<Post> getPostsBatch(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Collections.emptyList();

        return redisGuard.safeRead(
                () -> {
                    List<String> keys = postIds.stream()
                            .map(RedisKey.POST_DATA::build)
                            .collect(Collectors.toList());

                    List<Post> posts = postRedisTemplate.opsForValue().multiGet(keys);
                    if( posts == null|| posts.isEmpty() )return Collections.emptyList();
                    else return posts;
                },
                Collections.emptyList(),
                "getPostsBatch for " + postIds.size() + " posts"
        );
    }

    public Set<Long> getPostIdsBeforeCursor(Long teamId, Long cursorPostId, int limit) {
        return redisGuard.safeRead(
                () -> {
                    String indexKey = RedisKey.TEAM_POST_INDEX.build(teamId);

                    if (cursorPostId == null) {
                        return longRedisTemplate.opsForZSet().reverseRange(indexKey, 0, limit - 1);
                    }

                    return longRedisTemplate.opsForZSet().reverseRangeByScore(
                            indexKey, 0, cursorPostId - 1, 0, limit
                    );
                },
                Collections.emptySet(),
                "getPostIdsBeforeCursor for Team: " + teamId + " Limit: " + limit
        );
    }

    public void deletePostData(Long postId) {
        redisGuard.safeWrite(
                ()-> postRedisTemplate.delete(RedisKey.POST_DATA.build(postId)),
                "deletePostData for PostId: " + postId
        );

    }

    public void deletePostIndex(Long teamId , Long postId ) {
        redisGuard.safeWrite(
                ()-> postRedisTemplate.opsForZSet().remove(RedisKey.TEAM_POST_INDEX.build(teamId), postId),
                "remove postId : " + postId + " for Team: " + teamId
        );

    }


    public void savePostIndexes(List<Long> postIds , Long teamId){
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        redisGuard.safeWrite(()-> {
            String key = RedisKey.TEAM_POST_INDEX.build(teamId);
            Set<ZSetOperations.TypedTuple<Long>> tuples = new HashSet<>();
            for (Long postId : postIds) {
                if (postId != null) {
                    ZSetOperations.TypedTuple<Long> tuple = new DefaultTypedTuple<>(postId, postId.doubleValue());
                    tuples.add(tuple);
                }
            }

            if (!tuples.isEmpty()) {
                longRedisTemplate.opsForZSet().add(key, tuples);
            }
        } , "savePostIndexes for Team: " + teamId);
    }

}