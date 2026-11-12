package com.github.peacockxt.service.comment;

import com.github.peacockxt.dto.response.CommentResponse;
import com.github.peacockxt.infrastructure.cache.CommentRedisDao;
import com.github.peacockxt.models.Comment;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.policy.CommentPolicy;
import com.github.peacockxt.repository.implementation.CommentRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.PostProvider;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ReadCommentService {
    private final CommentRepository commentRepository;
    private final CommentRedisDao commentRedisDao;
    private final PostProvider postProvider;
    private final MembershipProvider membershipProvider;
    private final CommentPolicy commentPolicy;
    private final int cacheIndexLimit = 500;
    private final int defaultLimit = 50;
    private final String deleteStatus = "DELETED";

    public List<CommentResponse> readComment(Long postId , Long teamId , Long userId , Long membershipId , Long cursorId ){
        Post post = postProvider.getPost(postId);
        Membership  membership = membershipProvider.getMembership(membershipId);

        commentPolicy.validateReadComment(userId,teamId,membership,post);

        List<Long> commentIds = getCommentIds(postId,cursorId);
        List<Comment> commentList = getCommentInfo(commentIds);
        List<Long> replyIds = replyIdsOfCommentList(commentList);
        Map<Long,Comment> replyComment = new HashMap<>();
        if(!replyIds.isEmpty()){
            replyComment = commentMapConverter(getCommentInfo(replyIds));
        }
        return commentResponseListBuilder(commentList,replyComment);
    }

    private List<Long> getCommentIds(Long postId , Long cursorId){
        Set<Long> cacheCommentIds = commentRedisDao.getCommentIdsBeforeCursor(postId,cursorId,defaultLimit);
        List<Long> commentIds = new ArrayList<>(cacheCommentIds);
        if(!commentIds.isEmpty()){
            return  commentIds;
        }
        Pageable pageable = PageRequest.of(0,defaultLimit );
        if(cursorId==null){
            cursorId = Long.MAX_VALUE;
            pageable =  PageRequest.of(0,cacheIndexLimit);
        }
        commentIds = commentRepository.getCommentByCommentIdsByCursorId(cursorId,postId,pageable);
        if(cursorId.equals(Long.MAX_VALUE)) commentRedisDao.saveCommentIndexesBatch(postId,commentIds);
        return   commentIds.subList(0,Math.min(defaultLimit,commentIds.size()));
    }

    private List<Comment> getCommentInfo(List<Long> commentIds ){
        List<Comment> comments = commentRedisDao.getCommentsBatch(commentIds);
        List<Long> missingIds = new ArrayList<>();
        Map<Long,Comment> commentMap = new HashMap<>();
        for(int i=0;i<comments.size();i++){
            if(comments.get(i)==null){
                missingIds.add(commentIds.get(i));
            }
        }

        if (missingIds.isEmpty()) {
            return comments;
        }

        List<Comment> missingComments = commentRepository.getCommentByCommentIdIn(missingIds);
        for(Comment comment : missingComments){
            commentMap.put(comment.getCommentId(),comment);
        }

        if (!missingComments.isEmpty()) {
            commentRedisDao.saveCommentBatch(missingComments);
        }

        List<Comment> result = new ArrayList<>();

        for(int i=0;i<comments.size();i++){
            Comment comment = comments.get(i);
            if(comment==null){
                comment = commentMap.get(commentIds.get(i));
            }
            if(comment!=null)
                result.add(comment);
        }
        return result;
    }

    private List<Long> replyIdsOfCommentList(List<Comment> comments){
        List<Long> replyToIds = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (Comment comment : comments) {
            if (comment != null) {
                Long replyToId = comment.getReplyToId();
                if (replyToId != null && seenIds.add(replyToId)) {
                    replyToIds.add(replyToId);
                }
            }
        }
        return  replyToIds;
    }

    private Map<Long,Comment> commentMapConverter(List<Comment> comments){
        Map<Long,Comment> commentMap = new HashMap<>();
        for(Comment comment : comments){
            commentMap.put(comment.getCommentId(),comment);
        }
        return commentMap;
    }

    private List<CommentResponse> commentResponseListBuilder(List<Comment> comments
            , Map<Long,Comment> replyMap){
        List<CommentResponse> commentResponseList = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment == null ) continue;
            if (comment.getStatus().equals(deleteStatus)) continue;
            CommentResponse response = new CommentResponse();
            response.setCommentId(comment.getCommentId().toString());
            response.setContent(comment.getContent());
            response.setStatus(comment.getStatus());
            if (comment.getCreateBy() != null) {
                response.setSenderId(comment.getCreateBy().toString());
            }

            if (comment.getReplyToId() != null && replyMap.containsKey(comment.getReplyToId())) {
                Comment replyComment = replyMap.get(comment.getReplyToId());
                response.setReplyToId(replyComment.getCommentId().toString());
                response.setReplyContent(replyComment.getContent());
                if(replyComment.getCreateBy()!=null){
                    response.setReplierId(replyComment.getCreateBy().toString());
                }
            }
            commentResponseList.add(response);
        }
        return commentResponseList;
    }

}
