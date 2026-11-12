package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {
    @Query("select p from Post p where p.postId IN :postIds")
    List<Post> getPostsByPostIdIn(@Param("postIds")List<Long> postIds);
    @Query("Select p.postId from Post p where p.postId < :postId and p.team.teamId = :teamId and p.status != 'DELETE' " +
            " ORDER BY p.postId DESC ")
    List<Long> findPostIdByTeamIdAndPostIdLessThanOrderByPostIdDesc(@Param("teamId") Long teamId,
                                                    @Param("postId") Long postId ,
                                                    Pageable pageable);
}
