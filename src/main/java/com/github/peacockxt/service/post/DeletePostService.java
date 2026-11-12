package com.github.peacockxt.service.post;


import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.PostPayload;
import com.github.peacockxt.infrastructure.cache.PostRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.policy.PostPolicy;
import com.github.peacockxt.repository.implementation.PostRepository;
import com.github.peacockxt.service.provider.PostProvider;
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
public class DeletePostService {
    private final PostRepository postRepository;
    private final PostProvider postProvider;
    private final PostRedisDao postRedisDao;
    private final PostPolicy  postPolicy;
    private final LocalEventDispatcher  localEventDispatcher;
    private final String deletedStatus = "DELETED";
    private final String event_name = "DELETE_POST";

    @Transactional
    public void deletePost(Long postId , Long updaterId ,Long membershipId , Long teamId ) {
        Post post = postProvider.getPost(postId);
        postPolicy.validateCanDeletePost(post, updaterId, membershipId, teamId);
        post.setStatus(deletedStatus);
        post.setUpdateAt(LocalDateTime.now());
        postRepository.save(post);
        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(post,teamId));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                postRedisDao.deletePostData(postId);
                postRedisDao.deletePostIndex(teamId,postId);
            }
        } );
    }

    private InternalEvent<PostPayload> internalEventBuilder( Post post , long teamId){
        PostPayload postPayload = PostPayload.builder()
                .postId(post.getPostId())
                .teamId(teamId)
                .build();
        return InternalEvent.<PostPayload>builder()
                .eventType(event_name)
                .payload(postPayload)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }

}
