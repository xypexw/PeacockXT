package com.example.peacockxt.Service.PostModule;
import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Repository.Implimentation.PostRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class ReadingPostService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Post> postRedisTemplate;
    private final PostRepository postRepository;

    private final long maxRedisSetSize = 120 ;
    private final int postTtlDays = 5;
    private final String postKeyPrefix = "POST:";
    private final String teamKeyPrefix = "TEAM:";


    public ReadingPostService(RedisTemplate<String, Post> postRedisTemplate,
                               PostRepository postRepository,
                              StringRedisTemplate stringRedisTemplate) {
        this.postRedisTemplate = postRedisTemplate;
        this.postRepository = postRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }


     private List<String> readHotPosts(String teamId , int start, int end ) {
        String teamKey = getTeamKey(teamId);
        Set<String> hotId = stringRedisTemplate.opsForZSet().range(teamKeyPrefix + teamKey, start, end);
        return new ArrayList<>(hotId);
     }

     private List<Post> readPostsFromDatabase(String teamId , LocalDateTime pivotTime ){
        return postRepository.getPostsByIdWithPivot(teamId, pivotTime);
     }

     public List<Post> readPost(String teamId , LocalDateTime pivotTime , int limit){
        List<String> postIdsFromCache = readHotPosts(teamId,limit,limit+20);
        if(postIdsFromCache.isEmpty()){
            List<Post> posts = readPostsFromDatabase(teamId , pivotTime );
            for(Post post : posts){
                updateRedis(post, teamId);
            }
            return posts;
        }
        else{
            List<Post> posts = new ArrayList<>();
            for(String postId : postIdsFromCache){
                String postKey = getPostKey(postId);
                Post post = postRedisTemplate.opsForValue().get(postKey);
                if(post==null){
                    post = postRepository.findById(postId).get();
                    setPostToCache(post,postId);
                }
                posts.add(post);
            }
            return posts;
        }
     }

     public LocalDateTime getNextPivotTime( List<Post> posts ){
        return posts.getLast().getCreateAt();
     }

     private Long getMillis(LocalDateTime time ){
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
     }

    private void updateRedis(Post post, String teamId) {
        String postKey = getPostKey(post.getPostId());
        String teamKey = getTeamKey(teamId);

        long score = getMillis(post.getCreateAt());

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

    private void setPostToCache(Post post , String postId) {
        postRedisTemplate.opsForSet().add(getPostKey(postId), post);
    }

    private String getPostKey(String postId) {
        return postKeyPrefix + postId;
    }

    private String getTeamKey(String teamId) {
        return teamKeyPrefix + teamId;
    }

}
