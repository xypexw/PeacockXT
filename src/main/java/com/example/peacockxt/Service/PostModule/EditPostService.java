package com.example.peacockxt.Service.PostModule;

import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.PostRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EditPostService {

    private final String postKeyPrefix = "POST:";
    private final String editedStatus = "EDITED";
    private final RedisTemplate<String, Post> redisTemplate;
    private final PostRepository postRepository;


    public EditPostService(RedisTemplate<String, Post> redisTemplate, PostRepository postRepository) {
        this.redisTemplate = redisTemplate;
        this.postRepository = postRepository;
    }

    private void editPostFromDatabase(String postId, String title, String content , String userId ) {
        Post post =  postRepository.findById(postId).get();
        post.setTitle(title);
        post.setStatus(editedStatus);
        post.setDescription(content);
        post.setUpdatedAt(LocalDateTime.now());
        post.setUpdatedBy(userId);
        postRepository.save(post);
    }

    private void deletePostFormCache(String postId) {
        redisTemplate.delete(postKeyPrefix + postId);
    }

    public void editPost(String postId, String title, String content , String userId){
        editPostFromDatabase(postId, title, content, userId);
        deletePostFormCache(postId);
    }

    private String getPostKey(String postId) {
        return postKeyPrefix + postId;
    }

}
