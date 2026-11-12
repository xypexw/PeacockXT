package com.github.peacockxt.service.post;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.PostPayload;
import com.github.peacockxt.infrastructure.cache.PostRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.policy.PostPolicy;
import com.github.peacockxt.repository.implementation.PostRepository;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;



import java.time.LocalDateTime;

@Service
@Slf4j
@AllArgsConstructor
public class CreatePostService {
    private final PostRepository postRepository;
    private final TeamRepository teamRepository;
    private final PostPolicy postPolicy;
    private final PostRedisDao  postRedisDao;
    private final SnowflakeIdGenerator  snowflakeIdGenerator;
    private final String defaultStatus = "DEFAULT";
    private final String event_name = "CREATE_POST";
    private final LocalEventDispatcher localEventDispatcher;


    @Transactional
    public void createPost(Long userId , Long teamId , Long membershipId , String title , String description  ){
        postPolicy.validateCanCreatePost(userId , membershipId , teamId);
        Post post = postBuilder(teamId,title,description,userId);
        postRepository.save(post);
        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(post,teamId));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                savePostToCache(post,teamId);
            }
        });

    }

    private Post postBuilder(Long teamId , String title , String description , Long userId ){
        Team team   = teamRepository.getReferenceById(teamId);
        return Post.builder()
                .postId(snowflakeIdGenerator.nextId())
                .title(title)
                .description(description)
                .status(defaultStatus)
                .createAt(LocalDateTime.now())
                .createBy(userId)
                .updateAt(LocalDateTime.now())
                .updateBy(userId)
                .team(team)
                .build();
    }

    private InternalEvent<PostPayload> internalEventBuilder( Post post , Long teamId ){
        PostPayload payload = PostPayload.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .description(post.getDescription())
                .status(post.getStatus())
                .createAt(post.getCreateAt())
                .createBy(post.getCreateBy())
                .updateAt(post.getUpdateAt())
                .updateBy(post.getUpdateBy())
                .teamId(teamId)
                .build();
        return InternalEvent.<PostPayload>builder()
                .payload(payload)
                .eventType(event_name)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }

    private void savePostToCache(Post post ,  Long teamId ){
        postRedisDao.savePost(teamId,post);
    }

}
