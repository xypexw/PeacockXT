package com.github.peacockxt.repository_temp.implementation;

import com.github.peacockxt.models_temp.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {
    Comment getCommentByCommentId(Long commentId);

    @Query("select c.commentId from Comment c " +
            "where c.commentId < :cursorId AND c.post.postId = :postId AND c.status != 'DELETED'" +
            " ORDER BY c.commentId DESC ")
    List<Long> getCommentByCommentIdsByCursorId( @Param("cursorId") Long cursorId ,
                                                 @Param("postId") Long postId ,
                                                 Pageable pageable);

    @Query("select c from Comment c where c.commentId IN :commentIds")
    List<Comment> getCommentByCommentIdIn( @Param("commentIds") List<Long> commentIds);
}
