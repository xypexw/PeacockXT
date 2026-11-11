package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Infrastructure.Cache.PostRedisDao;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.Post;
import com.example.peacockxt.Repository.Imlpementation.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@AllArgsConstructor
public class PostProvider {
    private final PostRepository postRepository;
    private final PostRedisDao postRedisDao;

    public boolean hasPost(Long postId) {
        return getPost(postId) != null;
    }
    public Post getPost(Long postId) {
        // 1. Check Cache
        Post cachedPost = postRedisDao.getPostData(postId);
        if (cachedPost != null) {
            return cachedPost;
        }

        // 2. Cache Miss -> Query Database
        try {
            // Use default JpaRepository
            Post post = postRepository.findById(postId).orElse(null);

            // 3. If present in DB, repopulate into Cache
            if (post != null) {
                // Use single-element savePostBatch to update DATA without modifying ZSET index
                postRedisDao.savePostBatch(Collections.singletonList(post));
            }
            return post;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Post from database for postId: " + postId, e);
        }
    }
}