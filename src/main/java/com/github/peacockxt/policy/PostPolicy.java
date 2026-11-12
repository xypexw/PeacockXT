package com.github.peacockxt.policy;

import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamPermission;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PostPolicy {
    private final UserProvider userProvider;
    private final TeamProvider teamProvider;
    private final TeamPermission teamPermission;
    private final MembershipProvider membershipProvider;

    public void validateCanDeletePost(Post post, Long updaterId, Long membershipId, Long teamId) {
        if (!userProvider.hasUser(updaterId)) {
            throw new BusinessLogicException("User does not exist");
        }
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }
        if (teamPermission.isReader(membershipId, updaterId)) {
            throw new BusinessLogicException("User does not have permission");
        }
        if (post == null) {
            throw new BusinessLogicException("Post not found");
        }

        boolean isCreator = post.getCreateBy().equals(updaterId);
        boolean isAdmin = teamPermission.isTeamAdmin(membershipId, updaterId);

        if (!isCreator && !isAdmin) {
            throw new BusinessLogicException("User does not have permission");
        }
    }

    public void validateCanCreatePost( Long userId , Long membershipId, Long teamId) {
        if(!userProvider.hasUser(userId)){
            throw new BusinessLogicException("user does not exist");
        }
        if(!teamProvider.hasTeam(teamId)){
            throw new BusinessLogicException("team does not exist");
        }
        if(teamPermission.isReader(membershipId,userId)){
            throw new BusinessLogicException("team permission is reader");
        }

    }

    public void validateCanUpdatePost(Post post, Long updaterId, Long membershipId, Long teamId) {
        if(!userProvider.hasUser(updaterId)){
            throw new BusinessLogicException("user does not exist");
        }
        if(!teamProvider.hasTeam(teamId)){
            throw new BusinessLogicException("team does not exist");
        }
        if(teamPermission.isReader(membershipId,updaterId)){
            throw new BusinessLogicException("user dont have permission");
        }
        if(post == null){
            throw new BusinessLogicException("post not found");
        }
        if(!post.getCreateBy().equals(updaterId)){
            throw new BusinessLogicException("user is not post creator");
        }
    }

    public void validateCanReadPost( Long userId, Long membershipId, Long teamId) {
        if(!userProvider.hasUser(userId)){
            throw new BusinessLogicException("user does not exist");
        }
        if(!teamProvider.hasTeam(teamId)){
            throw new BusinessLogicException("team does not exist");
        }
        Membership membership = membershipProvider.getMembership(membershipId);
        if(membership == null ) throw new BusinessLogicException("membership not found");
        if(!membership.getTeamId().equals(teamId)) {
            throw new BusinessLogicException("membership is not belong to team");
        }
        if(!membership.getUserId().equals(userId)){
            throw new BusinessLogicException("membership is not belong to user");
        }
    }
    
}
