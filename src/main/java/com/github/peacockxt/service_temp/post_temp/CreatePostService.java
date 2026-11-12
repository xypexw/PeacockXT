package com.github.peacockxt.service_temp.post_temp;
import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.dto_temp.event_temp.PostPayload;
import com.github.peacockxt.infrastructure_temp.cache_temp.PostRedisDao;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.Post;
import com.github.peacockxt.models_temp.Team;
import com.github.peacockxt.policy_temp.PostPolicy;
import com.github.peacockxt.repository_temp.implementation.PostRepository;
import com.github.peacockxt.repository_temp.implementation.TeamRepository;
import com.github.peacockxt.service_temp.SnowflakeIdGenerator;
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
