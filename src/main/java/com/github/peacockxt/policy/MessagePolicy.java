package com.github.peacockxt.policy;

import com.github.peacockxt.infrastructure.cache.models.CacheChannel;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.models.Message;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.service.provider.ChannelProvider;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamPermission;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MessagePolicy {
    private static final String DELETED_STATUS = "DELETED";

    private final MembershipProvider membershipProvider;
    private final TeamPermission teamPermission;
    private final ChannelProvider channelProvider;
    private final UserProvider userProvider;

    public void validateCanDeleteMessage(Long channelId, Message message) {
        validateMessageAndChannel(channelId, message);

        if (!userProvider.hasUser(message.getSenderId())) {
            throw new BusinessLogicException("User is not authorized to delete this message");
        }
    }

    public void validateCanEditMessage(Long channelId, Message message) {
        validateMessageAndChannel(channelId, message);

        if (!userProvider.hasUser(message.getSenderId())) {
            throw new BusinessLogicException("User is not authorized to edit this message");
        }
    }

    private void validateMessageAndChannel(Long channelId, Message message) {
        CacheChannel channel = channelProvider.getChannel(channelId);
        if (channel == null) {
            throw new BusinessLogicException("Channel with id " + channelId + " does not exist");
        }
        if (DELETED_STATUS.equals(channel.getStatus())) {
            throw new BusinessLogicException("Channel with id " + channelId + " is DELETED");
        }
        if (message == null) {
            throw new BusinessLogicException("Message does not exist");
        }
    }

    public void validateCanCreateMessage(Long channelId, Long membershipId, Long userId) {
        CacheChannel channel = channelProvider.getChannel(channelId);
        if (channel == null) {
            throw new BusinessLogicException("Channel with id " + channelId + " does not exist");
        }
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User is not exist");
        }
        CacheMembership membership = membershipProvider.getMembership(membershipId);
        if (membership == null) {
            throw new BusinessLogicException("Membership does not exist");
        }
        if (teamPermission.isReader(membershipId, userId)) {
            throw new BusinessLogicException("creating message is not allowed for reader membership");
        }
        if (DELETED_STATUS.equals(channel.getStatus())) {
            throw new BusinessLogicException("creating message is not allowed for deleted channel");
        }
    }

    public void validateCanReadMessage(Long channelId, Long membershipId) {
        CacheChannel channel = channelProvider.getChannel(channelId);
        if (channel == null) {
            throw new BusinessLogicException("Channel with id " + channelId + " does not exist");
        }
        if (DELETED_STATUS.equals(channel.getStatus())) {
            throw new BusinessLogicException("read message is not allowed for deleted channel");
        }
        CacheMembership membership = membershipProvider.getMembership(membershipId);
        if (membership == null) {
            throw new BusinessLogicException("Membership does not exist");
        }
        if (!channel.getTeamId().equals(membership.getTeamId())) {
            throw new BusinessLogicException("membership is not belong to this team");
        }
    }
}
