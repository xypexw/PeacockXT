package com.github.peacockxt.policy;

import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import org.springframework.stereotype.Component;

@Component
public class CommentPolicy {

    private static final String DELETED_STATUS = "DELETED";
    private static final String READER_ROLE = "READER";

    public void validateCreateComment(Long userId, CacheMembership membership, CachePost post) {

        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }

        if (post == null) {
            throw new BusinessLogicException("Post is not found");
        }

        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        if (!membership.getTeamId().equals(post.getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to comment");
        }
    }

    public void validateUpdateComment(Long userId, CacheMembership membership, CachePost post, Comment comment) {
        if (comment == null || DELETED_STATUS.equals(comment.getStatus())) {
            throw new BusinessLogicException("Comment not found or deleted");
        }

        if (post == null || DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        if (!membership.getTeamId().equals(post.getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to update comment");
        }

        if (!comment.getCreateBy().equals(userId)) {
            throw new BusinessLogicException("Only the creator can edit this comment");
        }
    }

    public void validateReadComment(Long userId, Long teamId, CacheMembership membership, CachePost post) {
        if (post == null || membership == null) {
            throw new BusinessLogicException("Post or Membership not found");
        }
        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }
        if (!membership.getTeamId().equals(teamId)) {
            throw new BusinessLogicException("Team Id mismatch");
        }
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User Id mismatch");
        }

        if (!post.getTeamId().equals(teamId)) {
            throw new BusinessLogicException("Team Id of Post mismatch");
        }
    }
}
