package com.github.peacockxt.service_temp.provider_temp;

import com.github.peacockxt.infrastructure_temp.cache_temp.CommentRedisDao;
import com.github.peacockxt.models_temp.Comment;
import com.github.peacockxt.repository_temp.implementation.CommentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommentProvider {

    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;

    public Comment getComment(Long commentId) {
        // 1. Read Cache
        Comment comment = commentRedisDao.getCommentData(commentId);

        if (comment != null) {
            return comment;
        }
        comment = getCommentInDatabase(commentId);
        // 2. Cache Miss -> Read Database
        if (comment == null) {
            return null;
        }
        // 3. Write Cache
        commentRedisDao.saveCommentData(comment);
        return comment;
    }

    private Comment getCommentInDatabase(Long commentId) {
        return commentRepository.getCommentByCommentId(commentId);
    }


}