package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.event.CommentPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.policy.CommentPolicy;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.service.provider.CommentProvider;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class UpdateCommentService {
    private final CommentPolicy commentPolicy;
    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;
    private final CommentProvider commentProvider;
    private final PostProvider postProvider;
    private final MembershipProvider membershipProvider;

    private final LocalEventDispatcher localEventDispatcher;

    private final String EDITED_STATUS = "EDITED";
    private final String EVENT_NAME = "UPDATE_COMMENT";

    @Transactional
    public void updateComment(Long userId, Long membershipId, Long commentId, String newContent, Long postId) {
        // 1. Fetch Data
        Comment comment = commentProvider.getComment(commentId);
        Membership membership = membershipProvider.getMembership(membershipId);
        Post post = postProvider.getPost(postId);

        // 2. policy checking
        commentPolicy.validateUpdateComment(userId, membership, post, comment);

        // 3. Update Data
        comment.setContent(newContent);
        comment.setStatus(EDITED_STATUS);
        comment.setUpdateAt(LocalDateTime.now());
        comment.setUpdateBy(userId);

        commentRepository.save(comment);

        // 4. Push Event to RabbitMQ ( Edited Comment Notification )
        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, postId));

        // 5. Cache Sync after Commit DB Successfully
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                commentRedisDao.deleteCommentData(commentId);
            }
        });
    }

    private InternalEvent<CommentPayload> internalEventBuilder(Comment comment, Long postId) {
        CommentPayload payload = CommentPayload.builder()
                .commentId(comment.getCommentId())
                .postId(postId)
                .content(comment.getContent())
                .status(comment.getStatus())
                .updateAt(comment.getUpdateAt())
                .updateBy(comment.getUpdateBy())
                .build();

        return InternalEvent.<CommentPayload>builder()
                .eventType(EVENT_NAME)
                .payload(payload)
                .destination(WebsocketDestination.POST.getDestination(postId))
                .build();
    }
}