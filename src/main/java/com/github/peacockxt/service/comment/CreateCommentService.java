package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.event.CommentPayload; // Bạn nhớ tạo class DTO này nhé
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.policy.CommentPolicy;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CreateCommentService {
    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;
    private final PostProvider postProvider;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CommentPolicy commentPolicy;
    private final MembershipProvider membershipProvider;

    private final LocalEventDispatcher localEventDispatcher;

    private final String DEFAULT_STATUS = "DEFAULT";
    private final String EVENT_NAME = "CREATE_COMMENT";

    @Transactional
    public void createComment(Long userId, Long membershipId, Long postId, String content, Long replyToId) {
        Membership membership = membershipProvider.getMembership(membershipId);
        Post post = postProvider.getPost(postId);

        commentPolicy.validateCreateComment(userId, membership, post);

        Comment comment = commentBuilder(userId, post, content, replyToId);
        commentRepository.save(comment);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, postId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                commentRedisDao.saveComment(postId, comment);
            }
        });
    }

    private Comment commentBuilder(Long userId, Post post, String content, Long replyToId) {
        return Comment.builder()
                .commentId(snowflakeIdGenerator.nextId())
                .content(content)
                .status(DEFAULT_STATUS)
                .createAt(LocalDateTime.now())
                .createBy(userId)
                .updateAt(LocalDateTime.now())
                .updateBy(userId)
                .replyToId(replyToId)
                .post(post)
                .build();
    }

    private InternalEvent<CommentPayload> internalEventBuilder(Comment comment, Long postId) {
        CommentPayload payload = CommentPayload.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createAt(comment.getCreateAt())
                .createBy(comment.getCreateBy())
                .replyToId(comment.getReplyToId())
                .postId(postId)
                .build();

        return InternalEvent.<CommentPayload>builder()
                .eventType(EVENT_NAME)
                .payload(payload)
                .destination(WebsocketDestination.POST.getDestination(postId))
                .build();
    }
}