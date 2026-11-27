package com.github.peacockxt.service.post;

import com.github.peacockxt.dto.response.PostResponse;
import com.github.peacockxt.infrastructure.cache.PostRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.policy.PostPolicy;
import com.github.peacockxt.repository.implementation.PostRepository;
import com.github.peacockxt.service.provider.PostProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReadPostService {

    private final PostRepository postRepository;
    private final PostRedisDao postRedisDao;
    private final PostPolicy postPolicy;
    private final PostProvider postProvider;
    private final int limit = 40;
    private final int cacheLimit = 500;
    private static final String DELETED_STATUS = "DELETED";

    public List<PostResponse> getPosts(Long userId, Long membershipId, Long teamId, Long cursorPostId) {
        postPolicy.validateCanReadPost(userId, membershipId, teamId);

        Set<Long> cachedPostIds = postRedisDao.getPostIdsBeforeCursor(teamId, cursorPostId, limit);
        List<Long> postIdsToFetch = (cachedPostIds != null) ? new ArrayList<>(cachedPostIds) : new ArrayList<>();

        if (postIdsToFetch.isEmpty()) {
            if (cursorPostId == null) {
                List<Long> dbIds = postRepository.findPostIdByTeamIdAndPostIdLessThanOrderByPostIdDesc(
                        teamId, Long.MAX_VALUE, PageRequest.of(0, cacheLimit)
                );
                if (dbIds == null || dbIds.isEmpty()) {
                    return Collections.emptyList();
                }
                postRedisDao.savePostIndexes(dbIds, teamId);
                postIdsToFetch = dbIds.subList(0, Math.min(dbIds.size(), limit));
            } else {
                List<Long> dbIds = postRepository.findPostIdByTeamIdAndPostIdLessThanOrderByPostIdDesc(
                        teamId, cursorPostId, PageRequest.of(0, limit)
                );
                if (dbIds == null || dbIds.isEmpty()) {
                    return Collections.emptyList();
                }
                postIdsToFetch = dbIds;
            }
        }

        if (postIdsToFetch.isEmpty()) {
            return Collections.emptyList();
        }

        List<CachePost> cachedPosts = postRedisDao.getPostsBatch(postIdsToFetch);
        Map<Long, CachePost> postMap = new HashMap<>(postIdsToFetch.size());
        if (cachedPosts != null) {
            for (CachePost post : cachedPosts) {
                if (post != null) {
                    postMap.put(post.getPostId(), post);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long postId : postIdsToFetch) {
            if (!postMap.containsKey(postId)) {
                missingIds.add(postId);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Post> missingPostsFromDb = postRepository.getPostsByPostIdIn(missingIds);
            List<CachePost> toCache = new ArrayList<>(missingPostsFromDb.size());
            for (Post entity : missingPostsFromDb) {
                CachePost cachePost = toCachePost(entity);
                postMap.put(cachePost.getPostId(), cachePost);
                toCache.add(cachePost);
            }
            if (!toCache.isEmpty()) {
                postRedisDao.savePostBatch(toCache);
            }
        }

        List<PostResponse> responses = new ArrayList<>();
        for (Long postId : postIdsToFetch) {
            CachePost post = postMap.get(postId);
            if (post == null || DELETED_STATUS.equals(post.getStatus())) {
                continue;
            }
            responses.add(mapToResponse(post));
        }

        return responses;
    }

    public PostResponse getPostDetail(Long userId, Long membershipId, Long teamId, Long postId) {
        postPolicy.validateCanReadPost(userId, membershipId, teamId);

        CachePost post = postProvider.getPost(postId);
        if (post == null) {
            throw new BusinessLogicException("Post not found");
        }

        if (!teamId.equals(post.getTeamId())) {
            throw new BusinessLogicException("Post does not belong to this team");
        }

        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        return mapToResponse(post);
    }

    private PostResponse mapToResponse(CachePost post) {
        return PostResponse.builder()
                .postId(post.getPostId().toString())
                .title(post.getTitle())
                .description(post.getDescription())
                .status(post.getStatus())
                .createAt(post.getCreateAt())
                .updateAt(post.getUpdateAt())
                .createBy(post.getCreateBy() != null ? post.getCreateBy().toString() : null)
                .build();
    }

    private CachePost toCachePost(Post entity) {
        return CachePost.builder()
                .postId(entity.getPostId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createAt(entity.getCreateAt())
                .updateAt(entity.getUpdateAt())
                .createBy(entity.getCreateBy())
                .updateBy(entity.getUpdateBy())
                .teamId(entity.getTeamId())
                .build();
    }
}
