package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.PostRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.PostRepository;
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

    public CachePost getPost(Long postId) {
        if (postId == null) return null;
        CachePost cachedPost = postRedisDao.getPostData(postId);
        if (cachedPost != null) {
            return cachedPost;
        }

        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return null;
            }
            CachePost cachePost = CachePost.builder()
                    .postId(post.getPostId())
                    .title(post.getTitle())
                    .description(post.getDescription())
                    .status(post.getStatus())
                    .createAt(post.getCreateAt())
                    .updateAt(post.getUpdateAt())
                    .createBy(post.getCreateBy())
                    .updateBy(post.getUpdateBy())
                    .teamId(post.getTeamId())
                    .build();
            postRedisDao.savePostBatch(Collections.singletonList(cachePost));
            return cachePost;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Post from database for postId: " + postId, e);
        }
    }
}
