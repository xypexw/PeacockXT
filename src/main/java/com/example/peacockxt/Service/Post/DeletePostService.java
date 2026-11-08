package com.example.peacockxt.Service.Post;


import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.PostPayload;
import com.example.peacockxt.Infrastructure.Cache.PostRedisDao;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
import com.example.peacockxt.Infrastructure.MessageQueue.LocalEventDispatcher;
import com.example.peacockxt.Models.Post;
import com.example.peacockxt.Policy.PostPolicy;
import com.example.peacockxt.Repository.Imlpementation.PostRepository;
import com.example.peacockxt.Service.Provider.PostProvider;
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
