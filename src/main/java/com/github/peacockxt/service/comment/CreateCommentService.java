package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.event.CommentPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheComment;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.policy.CommentPolicy;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.repository.implementation.PostRepository;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
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
    private final PostRepository postRepository;
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
        CacheMembership membership = membershipProvider.getMembership(membershipId);
        CachePost post = postProvider.getPost(postId);

        commentPolicy.validateCreateComment(userId, membership, post);

        Comment comment = commentBuilder(userId, postId, content, replyToId);
        commentRepository.save(comment);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, postId));

        CacheComment cacheComment = CacheComment.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createBy(comment.getCreateBy())
                .updateBy(comment.getUpdateBy())
                .createAt(comment.getCreateAt())
                .updateAt(comment.getUpdateAt())
                .replyToId(comment.getReplyToId())
                .postId(postId)
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                commentRedisDao.saveComment(postId, cacheComment);
            }
        });
    }

    private Comment commentBuilder(Long userId, Long postId, String content, Long replyToId) {
        Post postRef = postRepository.getReferenceById(postId);
        return Comment.builder()
                .commentId(snowflakeIdGenerator.nextId())
                .content(content)
                .status(DEFAULT_STATUS)
                .createAt(LocalDateTime.now())
                .createBy(userId)
                .updateAt(LocalDateTime.now())
                .updateBy(userId)
                .replyToId(replyToId)
                .post(postRef)
                .postId(postId)
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
