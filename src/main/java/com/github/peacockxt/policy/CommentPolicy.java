package com.github.peacockxt.policy;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import org.springframework.stereotype.Component;

@Component
public class CommentPolicy {

    private static final String DELETED_STATUS = "DELETED";
    private static final String READER_ROLE = "READER";

    public void validateCreateComment(Long userId, Membership membership, Post post) {

        // 1. Check if Membership exists
        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }

        // 2. Check if Post exists
        if (post == null) {
            throw new BusinessLogicException("Post is not found");
        }

        // 3. Do not allow creating comments on deleted posts
        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        // 4. Ensure membership actually belongs to the user
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        // 5. Ensure membership and post belong to the same team
        if (!membership.getTeamId().equals(post.getTeam().getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        // 6. Check comment permission: READER role has read-only access and cannot create comments
        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to comment");
        }
    }

    public void validateUpdateComment(Long userId, Membership membership, Post post, Comment comment) {
        // 1. Check if Comment exists or has been deleted
        if (comment == null || DELETED_STATUS.equals(comment.getStatus())) {
            throw new BusinessLogicException("Comment not found or deleted");
        }

        // 2. Check if Post exists or has been deleted
        if (post == null || DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        // 3. Validate Membership
        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        // 4. Verify Team ID consistency between Membership and Post
        if (!membership.getTeam().getTeamId().equals(post.getTeam().getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        // 5. Check Member's permissions in the Team; Reader role cannot update comments
        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to update comment");
        }

        // 6. [MOST IMPORTANT]: Only the author (creator of the comment) is allowed to edit it
        if (!comment.getCreateBy().equals(userId)) {
            throw new BusinessLogicException("Only the creator can edit this comment");
        }
    }

    public void validateReadComment( Long userId , Long teamId , Membership membership, Post post) {
        if(post==null || membership==null){
            throw new BusinessLogicException("Post or Membership not found");
        }
        if(!membership.getTeamId().equals(teamId)){
            throw new BusinessLogicException("Team Id mismatch");
        }
        if(!membership.getUserId().equals(userId)){
            throw new BusinessLogicException("User Id mismatch");
        }

        if(!post.getTeam().getTeamId().equals(teamId)){
            throw new BusinessLogicException("Team Id of Post mismatch ");
        }
    }


}