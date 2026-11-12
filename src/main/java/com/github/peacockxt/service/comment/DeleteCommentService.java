package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.event.CommentPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.service.provider.CommentProvider;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
import com.github.peacockxt.service.provider.TeamPermission;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class DeleteCommentService {

    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;
    private final CommentProvider commentProvider;
    private final PostProvider postProvider;
    private final MembershipProvider membershipProvider;
    private final TeamPermission teamPermission;

    private final LocalEventDispatcher localEventDispatcher;

    private final String DELETED_CONTENT = "COMMENT HAS BEEN DELETED";
    private final String DELETED_STATUS = "DELETED";
    private final String READER_ROLE = "READER";
    private final String EVENT_NAME = "DELETE_COMMENT";

    @Transactional
    public void deleteComment(Long userId, Long membershipId, Long commentId) {

        // 1. Retrieve data and check existence
        Comment comment = commentProvider.getComment(commentId);
        if (comment == null || comment.getStatus().equals(DELETED_STATUS)) {
            throw new BusinessLogicException("Comment not found or already deleted");
        }

        Post post = postProvider.getPost(comment.getPost().getPostId());
        if (post == null || post.getStatus().equals(DELETED_STATUS)) {
            throw new BusinessLogicException("Post has been deleted");
        }

        Membership membership = membershipProvider.getMembership(membershipId);

        // 2. Validate basic business logic
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID not match");
        }

        if (!membership.getTeamId().equals(post.getTeam().getTeamId())) {
            throw new BusinessLogicException("Team ID not match");
        }

        if (membership.getRole().equals(READER_ROLE)) {
            throw new BusinessLogicException("User don't have permission");
        }

        // 3. Deletion authorization check: Must be the Creator OR Team Admin!
        boolean isCreator = comment.getCreateBy().equals(userId);
        boolean isAdmin = teamPermission.isTeamAdmin(membershipId, userId);

        if (!isCreator && !isAdmin) {
            throw new BusinessLogicException("User must be Creator or Team Admin to delete this comment");
        }

        // 4. Perform Soft Delete
        comment.setContent(DELETED_CONTENT);
        comment.setStatus(DELETED_STATUS);
        comment.setUpdateAt(LocalDateTime.now());
        comment.setUpdateBy(userId);

        commentRepository.save(comment);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, post.getPostId()));

        // 5. Synchronize cache after successful DB commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Invalidate/delete cache for this comment
                commentRedisDao.deleteCommentData(commentId);
            }
        });
    }

    private InternalEvent<CommentPayload> internalEventBuilder(Comment comment, Long postId) {
        CommentPayload payload = CommentPayload.builder()
                .commentId(comment.getCommentId())
                .postId(postId)
                .status(comment.getStatus()) // "DELETED"
                .content(comment.getContent()) // "COMMENT HAS BEEN DELETED"
                .build();

        return InternalEvent.<CommentPayload>builder()
                .eventType(EVENT_NAME)
                .payload(payload)
                .destination(WebsocketDestination.POST.getDestination(postId))
                .build();
    }
}