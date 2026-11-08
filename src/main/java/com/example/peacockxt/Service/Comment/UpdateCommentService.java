package com.example.peacockxt.Service.Comment;

import com.example.peacockxt.DTO.Event.CommentPayload;
import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.Infrastructure.Cache.CommentRedisDao;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.Comment;
import com.example.peacockxt.Models.Membership;
import com.example.peacockxt.Models.Post;
import com.example.peacockxt.Policy.CommentPolicy;
import com.example.peacockxt.Repository.Imlpementation.CommentRepository;
import com.example.peacockxt.Service.Provider.CommentProvider;
import com.example.peacockxt.Service.Provider.MembershipProvider;
import com.example.peacockxt.Service.Provider.PostProvider;
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
        // 1. Fetch dữ liệu
        Comment comment = commentProvider.getComment(commentId);
        Membership membership = membershipProvider.getMembership(membershipId);
        Post post = postProvider.getPost(postId);

        // 2. Kiểm tra policy
        commentPolicy.validateUpdateComment(userId, membership, post, comment);

        // 3. Cập nhật dữ liệu
        comment.setContent(newContent);
        comment.setStatus(EDITED_STATUS);
        comment.setUpdateAt(LocalDateTime.now());
        comment.setUpdateBy(userId);

        commentRepository.save(comment);

        // 4. Bắn Event ra RabbitMQ (Thông báo comment đã bị sửa)
        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(comment, postId));

        // 5. Đồng bộ Cache sau khi Commit DB thành công
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