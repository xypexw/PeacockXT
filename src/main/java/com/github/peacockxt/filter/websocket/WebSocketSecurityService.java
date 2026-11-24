package com.github.peacockxt.filter.websocket;
import com.github.peacockxt.dto.response.MembershipResponse;
import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.Post;
import com.github.peacockxt.models.PrivateSession;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.service.membership.ReadMembershipService;
import com.github.peacockxt.service.provider.ChannelProvider;
import com.github.peacockxt.service.provider.PostProvider;
import com.github.peacockxt.service.provider.PrivateSessionProvider;
import com.github.peacockxt.service.provider.TeamProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
public class WebSocketSecurityService {
    private final PostProvider postProvider;
    private final ChannelProvider channelProvider;
    private final TeamProvider teamProvider;
    private final ReadMembershipService readMembershipService;
    private final PrivateSessionProvider privateSessionProvider;

    Boolean teamAuthorized(Long userId , Long teamId) {
        if (userId == null || teamId == null) return false;

        Team team = teamProvider.getTeam(teamId);

        if(team == null)   return false;

        MembershipResponse membership =
                readMembershipService.getSingleMembership(userId, teamId);

        return membership != null;
    }

    Boolean postAuthorized(Long userId , Long postId) {
        if (userId == null || postId == null) return false;

        Post post = postProvider.getPost(postId);

        if(post == null)   return false;

        MembershipResponse membership =
                readMembershipService.getSingleMembership(userId,post.getTeam().getTeamId());

        return membership != null;
    }

    Boolean privateSessionAuthorized(Long userId, Long privateSessionId) {
        if (userId == null || privateSessionId == null) {
            return false;
        }

        PrivateSession privateSession = privateSessionProvider.getPrivateSession(privateSessionId);
        if (privateSession == null) {
            return false;
        }

        boolean isConnector = Objects.equals(privateSession.getConnectorId(), userId);
        boolean isReceiver = Objects.equals(privateSession.getReceiverId(), userId);

        return isConnector || isReceiver;
    }

    public boolean channelAuthorized(Long userId, Long channelId) {
        if (userId == null || channelId == null) return false;

        Channel chanel = channelProvider.getChannel(channelId);

        if(chanel == null) return false;

        MembershipResponse membership =
                readMembershipService.getSingleMembership(userId,chanel.getTeam().getTeamId());

        return membership != null;
    }

}
