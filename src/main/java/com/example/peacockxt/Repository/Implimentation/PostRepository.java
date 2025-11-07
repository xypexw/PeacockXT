package com.example.peacockxt.Repository.Implimentation;
import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post,String> {
    @Query(value = "SELECT * FROM post p WHERE p.team_id = :teamId AND p.create_at < :pivotTime" +
            " ORDER BY p.create_at DESC LIMIT 20",
            nativeQuery = true)
            List<Post> getPostsByIdWithPivot(
            @Param("teamId") String teamId,
            @Param("pivotTime") LocalDateTime pivotTime
    );

}
