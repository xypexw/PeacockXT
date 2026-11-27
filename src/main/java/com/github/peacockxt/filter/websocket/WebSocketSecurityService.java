package com.github.peacockxt.filter.websocket;

import com.github.peacockxt.infrastructure.cache.models.CacheChannel;
import com.github.peacockxt.infrastructure.cache.models.CachePost;
import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.infrastructure.cache.models.CacheTeam;
import com.github.peacockxt.service.provider.ChannelProvider;
import com.github.peacockxt.service.provider.MembershipProvider;
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
    private final MembershipProvider membershipProvider;
    private final PrivateSessionProvider privateSessionProvider;

    Boolean teamAuthorized(Long userId, Long teamId) {
        if (userId == null || teamId == null) return false;

        CacheTeam team = teamProvider.getTeam(teamId);
        if (team == null) return false;

        return membershipProvider.getMembershipByUserAndTeam(userId, teamId) != null;
    }

    Boolean postAuthorized(Long userId, Long postId) {
        if (userId == null || postId == null) return false;

        CachePost post = postProvider.getPost(postId);
        if (post == null || post.getTeamId() == null) return false;

        return membershipProvider.getMembershipByUserAndTeam(userId, post.getTeamId()) != null;
    }

    Boolean privateSessionAuthorized(Long userId, Long privateSessionId) {
        if (userId == null || privateSessionId == null) {
            return false;
        }

        CachePrivateSession privateSession = privateSessionProvider.getPrivateSession(privateSessionId);
        if (privateSession == null) {
            return false;
        }

        boolean isConnector = Objects.equals(privateSession.getConnectorId(), userId);
        boolean isReceiver = Objects.equals(privateSession.getReceiverId(), userId);

        return isConnector || isReceiver;
    }

    public boolean channelAuthorized(Long userId, Long channelId) {
        if (userId == null || channelId == null) return false;

        CacheChannel channel = channelProvider.getChannel(channelId);
        if (channel == null || channel.getTeamId() == null) return false;

        return membershipProvider.getMembershipByUserAndTeam(userId, channel.getTeamId()) != null;
    }
}
