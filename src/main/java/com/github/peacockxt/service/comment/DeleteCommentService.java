package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.event.CommentPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.CommentRepository;
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

        Comment comment = commentRepository.getCommentByCommentId(commentId);
        if (comment == null || DELETED_STATUS.equals(comment.getStatus())) {
            throw new BusinessLogicException("Comment not found or already deleted");
        }

        Long postId = comment.getPostId();
        CachePost post = postProvider.getPost(postId);
        if (post == null || DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        CacheMembership membership = membershipProvider.getMembership(membershipId);
        if (membership == null) {
            throw new BusinessLogicException("Membership not found");
        }

        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID not match");
        }

        if (!membership.getTeamId().equals(post.getTeamId())) {
            throw new BusinessLogicException("Team ID not match");
        }

        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User don't have permission");
        }

        boolean isCreator = comment.getCreateBy().equals(userId);
        boolean isAdmin = teamPermission.isTeamAdmin(membershipId, userId);

        if (!isCreator && !isAdmin) {
            throw new BusinessLogicException("User must be Creator or Team Admin to delete this comment");
        }

        comment.setContent(DELETED_CONTENT);
        comment.setStatus(DELETED_STATUS);
        comment.setUpdateAt(LocalDateTime.now());
        comment.setUpdateBy(userId);

        commentRepository.save(comment);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, post.getPostId()));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                commentRedisDao.deleteCommentData(commentId);
                commentRedisDao.deleteCommentIndex(postId, commentId);
            }
        });
    }

    private InternalEvent<CommentPayload> internalEventBuilder(Comment comment, Long postId) {
        CommentPayload payload = CommentPayload.builder()
                .commentId(comment.getCommentId())
                .postId(postId)
                .status(comment.getStatus())
                .content(comment.getContent())
                .build();

        return InternalEvent.<CommentPayload>builder()
                .eventType(EVENT_NAME)
                .payload(payload)
                .destination(WebsocketDestination.POST.getDestination(postId))
                .build();
    }
}
