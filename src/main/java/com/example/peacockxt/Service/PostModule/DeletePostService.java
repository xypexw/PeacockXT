package com.example.peacockxt.Service.PostModule;

import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Repository.Implimentation.PostRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

public class DeletePostService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Post> postRedisTemplate;
    private final PostRepository postRepository;

    private final String postKeyPrefix = "POST:";
    private final String teamKeyPrefix = "TEAM:";

    public DeletePostService(RedisTemplate<String, Post> postRedisTemplate,
                              PostRepository postRepository,
                                StringRedisTemplate stringRedisTemplate) {
        this.postRedisTemplate = postRedisTemplate;
        this.postRepository = postRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private void deleteFromCache(String postId , String teamId) {
        String postKey = getPostKey(postId);
        String teamKey = getTeamKey(teamId);
        stringRedisTemplate.opsForZSet().remove(teamKey,postId);
        postRedisTemplate.delete(postKey);
    }

    private void deleteFromDatabase(String postId ) {
        postRepository.deleteById(postId);
    }

    public void deletePost(String postId,String teamId){
        deleteFromDatabase(postId);
        deleteFromCache(postId,teamId);
    }

    private String getPostKey(String postId) {
        return postKeyPrefix + postId;
    }

    private String getTeamKey(String teamId) {
        return teamKeyPrefix + teamId;
    }

}
