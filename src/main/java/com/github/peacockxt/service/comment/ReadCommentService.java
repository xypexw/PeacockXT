package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.response.CommentResponse;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheComment;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.policy.CommentPolicy;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ReadCommentService {
    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;
    private final PostProvider postProvider;
    private final MembershipProvider membershipProvider;
    private final CommentPolicy commentPolicy;
    private final int cacheIndexLimit = 500;
    private final int defaultLimit = 50;
    private final String deleteStatus = "DELETED";

    public List<CommentResponse> readComment(Long postId, Long teamId, Long userId, Long membershipId, Long cursorId) {
        CachePost post = postProvider.getPost(postId);
        CacheMembership membership = membershipProvider.getMembership(membershipId);

        commentPolicy.validateReadComment(userId, teamId, membership, post);

        List<Long> commentIds = getCommentIds(postId, cursorId);
        if (commentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CacheComment> commentList = getCommentInfo(commentIds);
        List<Long> replyIds = replyIdsOfCommentList(commentList);
        Map<Long, CacheComment> replyComment = Collections.emptyMap();
        if (!replyIds.isEmpty()) {
            replyComment = commentMapConverter(getCommentInfo(replyIds));
        }
        return commentResponseListBuilder(commentList, replyComment);
    }

    private List<Long> getCommentIds(Long postId, Long cursorId) {
        Set<Long> cacheCommentIds = commentRedisDao.getCommentIdsBeforeCursor(postId, cursorId, defaultLimit);
        if (cacheCommentIds != null && !cacheCommentIds.isEmpty()) {
            return new ArrayList<>(cacheCommentIds);
        }

        if (cursorId == null) {
            Pageable pageable = PageRequest.of(0, cacheIndexLimit);
            List<Long> commentIds = commentRepository.getCommentByCommentIdsByCursorId(Long.MAX_VALUE, postId, pageable);
            if (commentIds == null || commentIds.isEmpty()) {
                return Collections.emptyList();
            }
            commentRedisDao.saveCommentIndexesBatch(postId, commentIds);
            return commentIds.subList(0, Math.min(defaultLimit, commentIds.size()));
        } else {
            Pageable pageable = PageRequest.of(0, defaultLimit);
            List<Long> commentIds = commentRepository.getCommentByCommentIdsByCursorId(cursorId, postId, pageable);
            return (commentIds == null) ? Collections.emptyList() : commentIds;
        }
    }

    private List<CacheComment> getCommentInfo(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheComment> cached = commentRedisDao.getCommentsBatch(commentIds);
        Map<Long, CacheComment> commentMap = new HashMap<>(commentIds.size());
        if (cached != null) {
            for (CacheComment c : cached) {
                if (c != null) {
                    commentMap.put(c.getCommentId(), c);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : commentIds) {
            if (!commentMap.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Comment> missingComments = commentRepository.getCommentByCommentIdIn(missingIds);
            List<CacheComment> toCache = new ArrayList<>(missingComments.size());
            for (Comment entity : missingComments) {
                CacheComment cacheComment = toCacheComment(entity);
                commentMap.put(cacheComment.getCommentId(), cacheComment);
                toCache.add(cacheComment);
            }
            if (!toCache.isEmpty()) {
                commentRedisDao.saveCommentBatch(toCache);
            }
        }

        List<CacheComment> result = new ArrayList<>(commentIds.size());
        for (Long id : commentIds) {
            CacheComment comment = commentMap.get(id);
            if (comment != null) {
                result.add(comment);
            }
        }
        return result;
    }

    private List<Long> replyIdsOfCommentList(List<CacheComment> comments) {
        List<Long> replyToIds = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (CacheComment comment : comments) {
            if (comment != null) {
                Long replyToId = comment.getReplyToId();
                if (replyToId != null && seenIds.add(replyToId)) {
                    replyToIds.add(replyToId);
                }
            }
        }
        return replyToIds;
    }

    private Map<Long, CacheComment> commentMapConverter(List<CacheComment> comments) {
        Map<Long, CacheComment> commentMap = new HashMap<>(comments.size());
        for (CacheComment comment : comments) {
            commentMap.put(comment.getCommentId(), comment);
        }
        return commentMap;
    }

    private List<CommentResponse> commentResponseListBuilder(List<CacheComment> comments,
                                                             Map<Long, CacheComment> replyMap) {
        List<CommentResponse> commentResponseList = new ArrayList<>();
        for (CacheComment comment : comments) {
            if (comment == null || deleteStatus.equals(comment.getStatus())) continue;
            CommentResponse response = new CommentResponse();
            response.setCommentId(comment.getCommentId().toString());
            response.setContent(comment.getContent());
            response.setStatus(comment.getStatus());
            if (comment.getCreateBy() != null) {
                response.setSenderId(comment.getCreateBy().toString());
            }

            if (comment.getReplyToId() != null && replyMap.containsKey(comment.getReplyToId())) {
                CacheComment replyComment = replyMap.get(comment.getReplyToId());
                response.setReplyToId(replyComment.getCommentId().toString());
                response.setReplyContent(replyComment.getContent());
                if (replyComment.getCreateBy() != null) {
                    response.setReplierId(replyComment.getCreateBy().toString());
                }
            }
            commentResponseList.add(response);
        }
        return commentResponseList;
    }

    private CacheComment toCacheComment(Comment entity) {
        return CacheComment.builder()
                .commentId(entity.getCommentId())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .updateBy(entity.getUpdateBy())
                .createAt(entity.getCreateAt())
                .updateAt(entity.getUpdateAt())
                .replyToId(entity.getReplyToId())
                .postId(entity.getPostId())
                .build();
    }
}
