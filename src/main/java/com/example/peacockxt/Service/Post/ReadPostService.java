package com.example.peacockxt.Service.Post;

import com.example.peacockxt.DTO.Respone.PostResponse;
import com.example.peacockxt.Infrastructure.Cache.PostRedisDao;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Post;
import com.example.peacockxt.Policy.PostPolicy;
import com.example.peacockxt.Repository.Imlpementation.PostRepository;
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
    private final int limit = 40;
    private final int cacheLimit = 500;
    private static final String DELETED_STATUS = "DELETED";

    public List<PostResponse> getPosts( Long userId, Long membershipId, Long teamId, Long cursorPostId) {
        postPolicy.validateCanReadPost(userId, membershipId, teamId);

        Set<Long> cachedPostIds = postRedisDao.getPostIdsBeforeCursor(teamId, cursorPostId, limit);

        List<Long> postIdsToFetch = new ArrayList<>(cachedPostIds);

        if (postIdsToFetch.isEmpty()) {
            if(cursorPostId==null)  cursorPostId = Long.MAX_VALUE;
            postIdsToFetch = postRepository.findPostIdByTeamIdAndPostIdLessThanOrderByPostIdDesc(teamId,
                    cursorPostId , PageRequest.of(0, cacheLimit));
            postRedisDao.savePostIndexes(postIdsToFetch, teamId);
            postIdsToFetch = postIdsToFetch.subList(0, Math.min(postIdsToFetch.size(), limit));
        }

        if (postIdsToFetch.isEmpty()) {
            return Collections.emptyList();
        }


        List<Post> cachedPosts = postRedisDao.getPostsBatch(postIdsToFetch);
        Set<Long> hitPostIds = new HashSet<>();

        if (cachedPosts != null) {
            for (Post post : cachedPosts) {
                if (post != null) {
                    hitPostIds.add(post.getPostId());
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();

        for (Long postId : postIdsToFetch) {
            if (!hitPostIds.contains(postId)) {
                missingIds.add(postId);
            }
        }

        List<Post> finalPosts = new ArrayList<>();

        if (cachedPosts != null) {
            for (Post post : cachedPosts) {
                if (post != null) {
                    finalPosts.add(post);
                }
            }
        }

        if (!missingIds.isEmpty()) {
            List<Post> missingPostsFromDb = postRepository.getPostsByPostIdIn(missingIds);
            if (!missingPostsFromDb.isEmpty()) {
                postRedisDao.savePostBatch(missingPostsFromDb);
                finalPosts.addAll(missingPostsFromDb);
            }
        }

        Map<Long, Post> postMap = new HashMap<>();
        for (Post post : finalPosts) {
            if (post != null) {
                postMap.put(post.getPostId(), post);
            }
        }

        List<PostResponse> responses = new ArrayList<>();

        for (Long postId : postIdsToFetch) {
            Post post = postMap.get(postId);

            if (post == null) {
                continue;
            }

            if (DELETED_STATUS.equals(post.getStatus())) {
                continue;
            }

            responses.add(mapToResponse(post));
        }

        return responses;
    }

    public PostResponse getPostDetail(Long userId, Long membershipId, Long teamId, Long postId) {
        postPolicy.validateCanReadPost(userId, membershipId, teamId);

        Post post = postRedisDao.getPostData(postId);

        if (post == null) {
            post = postRepository.findById(postId)
                    .orElseThrow(() ->
                            new BusinessLogicException("Post not found"));

            postRedisDao.savePostBatch(
                    Collections.singletonList(post)
            );
        }

        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        return mapToResponse(post);
    }

    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId().toString())
                .title(post.getTitle())
                .description(post.getDescription())
                .status(post.getStatus())
                .createAt(post.getCreateAt())
                .updateAt(post.getUpdateAt())
                .createBy(post.getCreateBy().toString())
                .build();
    }
}