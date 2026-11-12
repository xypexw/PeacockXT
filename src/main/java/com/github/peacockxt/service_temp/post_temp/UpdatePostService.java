package com.github.peacockxt.service_temp.post_temp;

import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.dto_temp.event_temp.PostPayload;
import com.github.peacockxt.infrastructure_temp.cache_temp.PostRedisDao;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.Post;
import com.github.peacockxt.policy_temp.PostPolicy;
import com.github.peacockxt.repository_temp.implementation.PostRepository;
import com.github.peacockxt.service_temp.provider_temp.PostProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;


@Service
@Slf4j
@AllArgsConstructor
public class UpdatePostService {
    private final PostRepository postRepository;
    private final PostPolicy postPolicy;
    private final PostProvider postProvider;
    private final PostRedisDao postRedisDao;
    private final LocalEventDispatcher localEventDispatcher;
    private final String updatedStatus = "UPDATED";
    private final String event_name = "UPDATE_POST";

    @Transactional
    public void updatePost(Long postId , Long updaterId , String title ,
                           String description , Long membershipId , Long teamId ) {
        Post post = postProvider.getPost(postId);
        postPolicy.validateCanUpdatePost(post,updaterId,membershipId,teamId);
        post.setStatus(updatedStatus);
        post.setUpdateAt(LocalDateTime.now());
        post.setTitle(title);
        post.setDescription(description);
        postRepository.save(post);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(post,teamId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                postRedisDao.deletePostData(postId);
            }
        } );

    }

    InternalEvent<PostPayload> internalEventBuilder( Post post , Long teamId ){
        PostPayload postPayload = PostPayload.builder()
                .postId(post.getPostId())
                .teamId(teamId)
                .updateBy(post.getUpdateBy())
                .updateAt(post.getUpdateAt())
                .createAt(post.getCreateAt())
                .title(post.getTitle())
                .description(post.getDescription())
                .build();
        return InternalEvent.<PostPayload>builder()
                .eventType(event_name)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .payload(postPayload)
                .build();
    }

}
