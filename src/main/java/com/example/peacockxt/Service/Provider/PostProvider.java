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
        // 1. Quét Cache
        Post cachedPost = postRedisDao.getPostData(postId);
        if (cachedPost != null) {
            return cachedPost;
        }

        // 2. Cache Miss -> Lội DB
        try {
            // Xài JpaRepository mặc định
            Post post = postRepository.findById(postId).orElse(null);

            // 3. Có trong DB thì nhét lại vào Cache
            if (post != null) {
                // Dùng savePostBatch 1 phần tử để cập nhật DATA, không đụng tới ZSET index
                postRedisDao.savePostBatch(Collections.singletonList(post));
            }
            return post;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Post from database for postId: " + postId, e);
        }
    }
}
