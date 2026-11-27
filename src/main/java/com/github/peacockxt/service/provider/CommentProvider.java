package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheComment;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.repository.implementation.CommentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommentProvider {

    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;

    public CacheComment getComment(Long commentId) {
        if (commentId == null) return null;
        CacheComment cached = commentRedisDao.getCommentData(commentId);
        if (cached != null) {
            return cached;
        }
        Comment comment = getCommentInDatabase(commentId);
        if (comment == null) {
            return null;
        }
        CacheComment cacheComment = toCacheComment(comment);
        commentRedisDao.saveCommentData(cacheComment);
        return cacheComment;
    }

    private Comment getCommentInDatabase(Long commentId) {
        return commentRepository.getCommentByCommentId(commentId);
    }

    private CacheComment toCacheComment(Comment comment) {
        return CacheComment.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createBy(comment.getCreateBy())
                .updateBy(comment.getUpdateBy())
                .createAt(comment.getCreateAt())
                .updateAt(comment.getUpdateAt())
                .replyToId(comment.getReplyToId())
                .postId(comment.getPostId())
                .build();
    }
}
