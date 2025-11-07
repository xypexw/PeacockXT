package com.example.peacockxt.Service.PostModule;

import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.PostRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class CreatingPostService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Post> postRedisTemplate;
    private final PostRepository postRepository;


    private final int postTtlDays = 5;
    private final long maxRedisSetSize = 120 ;

    private final String postKeyPrefix = "POST:";
    private final String teamKeyPrefix = "TEAM:";

    private final String defaultStatus = "NORMAL";

    public CreatingPostService(RedisTemplate<String, Post> postRedisTemplate,
                               PostRepository postRepository,
                               StringRedisTemplate stringRedisTemplate) {
        this.postRedisTemplate = postRedisTemplate;
        this.postRepository = postRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public void createPost(String userId, String title, String content, LocalDateTime currentTime, Team team) {
        String postId = UuidCreator.getTimeOrdered().toString();
        Post post = buildPost(postId, userId, title, content, currentTime, team);

        savePostToDb(post);
        updateRedis(post, team);
    }

    private Post buildPost(String postId, String userId, String title, String content,
                           LocalDateTime currentTime, Team team) {
        return Post.builder()
                .postId(postId)
                .title(title)
                .description(content)
                .createAt(currentTime)
                .updatedAt(currentTime)
                .createBy(userId)
                .updatedBy(userId)
                .status(defaultStatus)
                .team(team)
                .build();
    }

    private void savePostToDb(Post post) {
        postRepository.save(post);
    }

    private void updateRedis(Post post, Team team) {
        String postKey = getPostKey(post.getPostId());
        String teamKey = getTeamKey(team.getTeamId());

        long score = System.currentTimeMillis();

        // Add postId to sorted set
        stringRedisTemplate.opsForZSet().add(teamKey, post.getPostId(), score);

        // Set post object in Redis with TTL
        postRedisTemplate.opsForValue().setIfAbsent(postKey, post, postTtlDays, TimeUnit.DAYS);

        trimRedisZSetIfNeeded(teamKey);
    }

    private void trimRedisZSetIfNeeded(String teamKey) {
        Long currentSize = stringRedisTemplate.opsForZSet().zCard(teamKey);
        if (currentSize != null && currentSize > maxRedisSetSize) {
            long excess = currentSize - maxRedisSetSize;
            stringRedisTemplate.opsForZSet().removeRange(teamKey, 0, excess - 1);
        }
    }

    private String getPostKey(String postId) {
        return postKeyPrefix + postId;
    }

    private String getTeamKey(String teamId) {
        return teamKeyPrefix + teamId;
    }
}
